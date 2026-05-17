package androidtoolkit.backend.crash;

import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.agent.AgentCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.*;

/**
 * Triggers automatic log pulls from devices when crashes are detected and auto-pull is enabled.
 * Prevents concurrent pulls on the same device using a lock map.
 */
@Service
public class AutoPullService {

    private static final Logger log = LoggerFactory.getLogger(AutoPullService.class);

    private static final long PULL_TIMEOUT_SECONDS = 60;

    private final AgentConnectionManager agentConnectionManager;
    private final AlertConfigurationService alertConfigurationService;
    private final CrashEventRepository crashEventRepository;
    private final ConcurrentHashMap<String, CompletableFuture<String>> inProgressPulls = new ConcurrentHashMap<>();
    private final ExecutorService pullExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public AutoPullService(AgentConnectionManager agentConnectionManager,
                           AlertConfigurationService alertConfigurationService,
                           CrashEventRepository crashEventRepository) {
        this.agentConnectionManager = agentConnectionManager;
        this.alertConfigurationService = alertConfigurationService;
        this.crashEventRepository = crashEventRepository;
    }

    /**
     * Triggers a log pull for the crashed device if auto-pull is enabled and no pull is in progress.
     */
    public void triggerLogPull(CrashEvent event) {
        AlertConfigurationEntity config = alertConfigurationService.getForProject(event.projectId());

        if (!config.isAutoPullEnabled()) {
            log.debug("Auto-pull disabled for project {}, skipping", event.projectId());
            return;
        }

        if (isPullInProgress(event.deviceSerial())) {
            log.debug("Pull already in progress for device {}, skipping", event.deviceSerial());
            return;
        }

        CompletableFuture<String> pullFuture = new CompletableFuture<>();
        CompletableFuture<String> existing = inProgressPulls.putIfAbsent(event.deviceSerial(), pullFuture);

        if (existing != null) {
            // Another thread beat us to it
            log.debug("Concurrent pull detected for device {}, skipping", event.deviceSerial());
            return;
        }

        pullExecutor.submit(() -> {
            try {
                executePull(event);
                pullFuture.complete(event.deviceSerial());
            } catch (Exception e) {
                log.warn("Auto-pull failed for device {}: {}", event.deviceSerial(), e.getMessage());
                recordPullFailure(event, e.getMessage());
                pullFuture.completeExceptionally(e);
            } finally {
                inProgressPulls.remove(event.deviceSerial());
            }
        });

        // Set timeout to release the lock
        pullFuture.orTimeout(PULL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException) {
                        log.warn("Auto-pull timed out for device {}", event.deviceSerial());
                        recordPullFailure(event, "Pull timed out after " + PULL_TIMEOUT_SECONDS + "s");
                        inProgressPulls.remove(event.deviceSerial());
                    }
                    return null;
                });
    }

    /**
     * Returns true if a log pull is currently in progress for the given device.
     */
    public boolean isPullInProgress(String deviceSerial) {
        return inProgressPulls.containsKey(deviceSerial);
    }

    private void executePull(CrashEvent event) {
        Optional<String> agentId = agentConnectionManager.findAgentForDevice(
                event.tenantId(), event.deviceSerial());

        if (agentId.isEmpty()) {
            throw new RuntimeException("No agent found for device " + event.deviceSerial());
        }

        String targetPath = String.format("crash-pull/%s/%s", event.deviceSerial(), event.id());
        AgentCommand pullCommand = new AgentCommand.PullLogs(event.deviceSerial(), targetPath);

        agentConnectionManager.sendToAgent(agentId.get(), pullCommand);
        log.info("Auto-pull command sent for device {} (crash event {})", event.deviceSerial(), event.id());
    }

    private void recordPullFailure(CrashEvent event, String reason) {
        try {
            crashEventRepository.findById(Long.parseLong(event.id())).ifPresent(entity -> {
                entity.setAutoPullLogPath("FAILED: " + reason);
                crashEventRepository.save(entity);
            });
        } catch (NumberFormatException e) {
            // Event ID is a UUID string, not a database ID yet — skip recording
            log.debug("Cannot record pull failure: crash event not yet persisted");
        }
    }
}
