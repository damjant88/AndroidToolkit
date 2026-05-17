package androidtoolkit.backend.crash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.List;

/**
 * Spring event listener that reacts to CrashDetectedEvent and dispatches to
 * CrashHistoryService, CrashLogCaptureService, CrashNotificationService, and AutoPullService.
 * Each handler is independent — failures in one do not block others.
 */
@Component
public class CrashEventListener {

    private static final Logger log = LoggerFactory.getLogger(CrashEventListener.class);

    private final CrashHistoryService crashHistoryService;
    private final CrashLogCaptureService crashLogCaptureService;
    private final CrashNotificationService crashNotificationService;
    private final AutoPullService autoPullService;

    public CrashEventListener(CrashHistoryService crashHistoryService,
                              CrashLogCaptureService crashLogCaptureService,
                              CrashNotificationService crashNotificationService,
                              AutoPullService autoPullService) {
        this.crashHistoryService = crashHistoryService;
        this.crashLogCaptureService = crashLogCaptureService;
        this.crashNotificationService = crashNotificationService;
        this.autoPullService = autoPullService;
    }

    @EventListener
    public void handleCrashDetected(CrashDetectedEvent event) {
        CrashEvent crashEvent = event.crashEvent();
        log.info("Crash detected on device {}: {} ({})",
                crashEvent.deviceSerial(), crashEvent.crashType(), crashEvent.severity());

        // Persist the crash event
        persistCrashEvent(crashEvent);

        // Capture crash log
        captureCrashLog(crashEvent, event.lineBuffer(), event.postCrashLines());

        // Send notification
        sendNotification(crashEvent);

        // Trigger auto-pull if configured
        triggerAutoPull(crashEvent);
    }

    private void persistCrashEvent(CrashEvent crashEvent) {
        try {
            crashHistoryService.persist(crashEvent);
        } catch (Exception e) {
            log.error("Failed to persist crash event for device {}: {}",
                    crashEvent.deviceSerial(), e.getMessage());
        }
    }

    private void captureCrashLog(CrashEvent crashEvent, Deque<String> lineBuffer, List<String> postCrashLines) {
        try {
            if (lineBuffer != null && !lineBuffer.isEmpty()) {
                String path = crashLogCaptureService.captureCrashLog(crashEvent, lineBuffer, postCrashLines);
                log.debug("Crash log captured at: {}", path);
            }
        } catch (Exception e) {
            log.error("Failed to capture crash log for device {}: {}",
                    crashEvent.deviceSerial(), e.getMessage());
        }
    }

    private void sendNotification(CrashEvent crashEvent) {
        try {
            crashNotificationService.notifyCrash(crashEvent);
        } catch (Exception e) {
            log.warn("Failed to send crash notification for device {}: {}",
                    crashEvent.deviceSerial(), e.getMessage());
        }
    }

    private void triggerAutoPull(CrashEvent crashEvent) {
        try {
            autoPullService.triggerLogPull(crashEvent);
        } catch (Exception e) {
            log.warn("Failed to trigger auto-pull for device {}: {}",
                    crashEvent.deviceSerial(), e.getMessage());
        }
    }
}
