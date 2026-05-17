package androidtoolkit.backend.device;

import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.agent.AgentCommand;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages logcat stream lifecycle in SaaS mode.
 * Tracks active streams and handles agent disconnection cleanup.
 *
 * <p>Active only in SaaS deployment mode where logcat output is relayed
 * from remote agents to frontend clients via WebSocket STOMP topics.
 */
@Component
@ConditionalOnProperty(name = "deployment.mode", havingValue = "saas")
public class LogcatRelayService {

    private static final int MAX_LINE_LENGTH = 4096;

    private final AgentConnectionManager connectionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> activeStreams = ConcurrentHashMap.newKeySet();

    public LogcatRelayService(AgentConnectionManager connectionManager,
                              SimpMessagingTemplate messagingTemplate) {
        this.connectionManager = connectionManager;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Starts a logcat stream for the specified device by locating the owning agent
     * and sending a StartLogcat command. Adds the stream to the active streams registry.
     *
     * @param tenantId the tenant that owns the device
     * @param serial   the device serial number
     * @throws DeviceUnreachableException if no agent is connected for the device
     */
    public void startStream(Long tenantId, String serial) {
        String agentId = connectionManager.findAgentForDevice(tenantId, serial)
                .orElseThrow(() -> new DeviceUnreachableException(serial,
                        "No agent connected for device"));

        String requestId = UUID.randomUUID().toString();
        AgentCommand.StartLogcat command = new AgentCommand.StartLogcat(serial, requestId);
        connectionManager.sendToAgent(agentId, command);
        activeStreams.add(serial);
    }

    /**
     * Stops a logcat stream for the specified device by sending a StopLogcat command
     * to the owning agent and removing the stream from the active streams registry.
     *
     * @param tenantId the tenant that owns the device
     * @param serial   the device serial number
     */
    public void stopStream(Long tenantId, String serial) {
        String agentId = connectionManager.findAgentForDevice(tenantId, serial)
                .orElse(null);

        if (agentId != null) {
            AgentCommand.StopLogcat command = new AgentCommand.StopLogcat(serial);
            connectionManager.sendToAgent(agentId, command);
        }

        activeStreams.remove(serial);
    }

    /**
     * Forwards a logcat line to the appropriate WebSocket topic. Lines exceeding
     * 4096 characters are truncated to exactly 4096 characters.
     *
     * @param serial    the device serial number
     * @param line      the logcat line text
     * @param timestamp the server-received timestamp in milliseconds
     */
    public void forwardLine(String serial, String line, long timestamp) {
        String truncatedLine = line.length() > MAX_LINE_LENGTH
                ? line.substring(0, MAX_LINE_LENGTH)
                : line;

        Map<String, Object> message = Map.of(
                "serial", serial,
                "line", truncatedLine,
                "timestamp", timestamp
        );

        messagingTemplate.convertAndSend("/topic/logcat/" + serial, message);
    }

    /**
     * Handles agent disconnection by sending stream-ended notifications for all
     * active streams belonging to devices of the disconnected agent, then removes
     * those streams from the active streams registry.
     *
     * @param agentId the identifier of the disconnected agent
     */
    public void onAgentDisconnected(String agentId) {
        Long tenantId = connectionManager.getTenantIdForAgent(agentId);
        if (tenantId == null) {
            return;
        }

        Set<String> agentDevices = getAgentDeviceSerials(agentId);

        for (String serial : agentDevices) {
            if (activeStreams.remove(serial)) {
                Map<String, Object> notification = Map.of(
                        "serial", serial,
                        "type", "stream-ended",
                        "reason", "agent_disconnected"
                );
                messagingTemplate.convertAndSend("/topic/logcat/" + serial, notification);
            }
        }
    }

    /**
     * Returns the set of device serials currently being streamed.
     * Useful for monitoring and testing.
     *
     * @return unmodifiable view of active stream serials
     */
    public Set<String> getActiveStreams() {
        return Set.copyOf(activeStreams);
    }

    /**
     * Retrieves the device serials associated with a specific agent.
     * Uses AgentConnectionManager to look up the agent's registered devices.
     */
    private Set<String> getAgentDeviceSerials(String agentId) {
        // Query the connection manager for devices belonging to this agent.
        // We get the tenant's full device list and filter by checking which devices
        // are owned by this specific agent.
        Long tenantId = connectionManager.getTenantIdForAgent(agentId);
        if (tenantId == null) {
            return Set.of();
        }

        var discoveryResult = connectionManager.getDevicesForTenant(tenantId);
        Set<String> agentSerials = ConcurrentHashMap.newKeySet();

        for (String serial : discoveryResult.getSerials()) {
            var owningAgent = connectionManager.findAgentForDevice(tenantId, serial);
            if (owningAgent.isPresent() && owningAgent.get().equals(agentId)) {
                agentSerials.add(serial);
            }
        }

        return agentSerials;
    }
}
