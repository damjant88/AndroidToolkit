package androidtoolkit.backend.crash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Pushes crash alerts to the frontend via STOMP WebSocket.
 * Sends notifications to device-specific and project-level topics.
 * Respects per-project notification preferences from AlertConfiguration.
 */
@Service
public class CrashNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CrashNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final AlertConfigurationService alertConfigurationService;

    public CrashNotificationService(SimpMessagingTemplate messagingTemplate,
                                    AlertConfigurationService alertConfigurationService) {
        this.messagingTemplate = messagingTemplate;
        this.alertConfigurationService = alertConfigurationService;
    }

    /**
     * Sends a crash notification to the frontend if the project's notification preferences allow it.
     */
    public void notifyCrash(CrashEvent event) {
        AlertConfigurationEntity config = alertConfigurationService.getForProject(event.projectId());
        NotificationPreference pref = config.getNotificationPreference();

        if (pref == NotificationPreference.NONE) {
            log.debug("Notifications disabled for project {}, skipping", event.projectId());
            return;
        }

        Map<String, Object> payload = buildPayload(event);

        try {
            // Send to device-specific topic
            messagingTemplate.convertAndSend(
                    "/topic/crash/" + event.deviceSerial(), payload);

            // Send crash count update to project topic
            messagingTemplate.convertAndSend(
                    "/topic/crash-count/" + event.projectId(),
                    Map.of("projectId", event.projectId(), "deviceSerial", event.deviceSerial()));

            log.debug("Crash notification sent for device {} (project {})",
                    event.deviceSerial(), event.projectId());
        } catch (Exception e) {
            log.warn("Failed to send crash notification for device {}: {}",
                    event.deviceSerial(), e.getMessage());
        }
    }

    /**
     * Builds the notification payload containing device name, crash type, and package name.
     */
    public Map<String, Object> buildPayload(CrashEvent event) {
        return Map.of(
                "id", event.id(),
                "deviceSerial", event.deviceSerial(),
                "deviceName", event.deviceName() != null ? event.deviceName() : event.deviceSerial(),
                "crashType", event.crashType(),
                "packageName", event.packageName() != null ? event.packageName() : "unknown",
                "severity", event.severity().name(),
                "timestamp", event.timestamp().toString()
        );
    }
}
