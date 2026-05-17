package androidtoolkit.backend.device;

import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.agent.AgentCommand;
import androidtoolkit.domain.agent.AgentMessage.OperationResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sends commands to agents via WebSocket and awaits OperationResult responses.
 * Each command is assigned a unique requestId; the corresponding future is
 * completed when the agent responds or times out after 30 seconds.
 *
 * <p>Active only in SaaS deployment mode where commands must be relayed
 * to remote agents rather than executed locally via adb.
 */
@Component
@ConditionalOnProperty(name = "deployment.mode", havingValue = "saas")
public class CommandRelay {

    private static final long COMMAND_TIMEOUT_SECONDS = 30;

    private final AgentConnectionManager connectionManager;
    private final ConcurrentHashMap<String, PendingCommand> pendingCommands = new ConcurrentHashMap<>();

    /**
     * Internal record representing a command that has been sent to an agent
     * and is awaiting an OperationResult response.
     */
    record PendingCommand(
            String requestId,
            String agentId,
            String deviceSerial,
            AgentCommand command,
            Instant sentAt,
            CompletableFuture<OperationResult> future
    ) {}

    public CommandRelay(AgentConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Sends a command to the agent that owns the specified device and awaits
     * the OperationResult response with a 30-second timeout.
     *
     * @param tenantId the tenant that owns the device
     * @param serial   the device serial number
     * @param command  the command to send to the agent
     * @return the OperationResult from the agent
     * @throws DeviceUnreachableException if no agent is connected for the device
     * @throws CommandTimeoutException    if the agent does not respond within 30 seconds
     * @throws AgentDisconnectedException if the agent disconnects during command execution
     */
    public OperationResult execute(Long tenantId, String serial, AgentCommand command) {
        String agentId = connectionManager.findAgentForDevice(tenantId, serial)
                .orElseThrow(() -> new DeviceUnreachableException(serial,
                        "No agent connected for device"));

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<OperationResult> future = new CompletableFuture<>();
        future.orTimeout(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        PendingCommand pending = new PendingCommand(
                requestId, agentId, serial, command, Instant.now(), future);
        pendingCommands.put(requestId, pending);

        try {
            connectionManager.sendToAgent(agentId, command);
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                throw new CommandTimeoutException(serial, COMMAND_TIMEOUT_SECONDS);
            }
            if (e.getCause() instanceof AgentDisconnectedException ade) {
                throw ade;
            }
            throw new RuntimeException("Command execution failed for device " + serial, e.getCause());
        } finally {
            pendingCommands.remove(requestId);
        }
    }

    /**
     * Completes the pending future for the given requestId with the provided result.
     * Called by AgentWebSocketHandler when an OperationResult message arrives from an agent.
     *
     * @param requestId the request identifier from the OperationResult message
     * @param result    the operation result from the agent
     */
    public void completeCommand(String requestId, OperationResult result) {
        PendingCommand pending = pendingCommands.get(requestId);
        if (pending != null) {
            pending.future().complete(result);
        }
    }

    /**
     * Completes all pending futures for the specified agent exceptionally with
     * {@link AgentDisconnectedException}. Called when an agent WebSocket connection
     * is lost to ensure callers waiting on command results are notified promptly.
     *
     * @param agentId the identifier of the disconnected agent
     */
    public void failCommandsForAgent(String agentId) {
        for (Map.Entry<String, PendingCommand> entry : pendingCommands.entrySet()) {
            PendingCommand pending = entry.getValue();
            if (pending.agentId().equals(agentId)) {
                pending.future().completeExceptionally(
                        new AgentDisconnectedException(pending.deviceSerial(), agentId));
            }
        }
    }

    /**
     * Returns the number of pending commands. Useful for monitoring and testing.
     *
     * @return the count of pending commands awaiting responses
     */
    public int getPendingCommandCount() {
        return pendingCommands.size();
    }
}
