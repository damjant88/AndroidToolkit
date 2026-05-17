package androidtoolkit.backend.crash;

import net.jqwik.api.*;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link CrashNotificationService}.
 * Covers Property 9 from the design document.
 */
@Tag("Feature: crash-detection-alerts, Property 9: Notification payload correctness")
class CrashNotificationPropertyTest {

    /**
     * Property 9: Notification payload correctness
     * For any CrashEvent where notifications are enabled, the payload contains
     * device name, crash type, and package name.
     */
    @Property(tries = 100)
    void payloadContainsRequiredFields(@ForAll("crashEvents") CrashEvent event) {
        AlertConfigurationService configService = mock(AlertConfigurationService.class);
        org.springframework.messaging.simp.SimpMessagingTemplate template =
                mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);

        AlertConfigurationEntity config = new AlertConfigurationEntity();
        config.setNotificationPreference(NotificationPreference.BROWSER);
        when(configService.getForProject(event.projectId())).thenReturn(config);

        CrashNotificationService service = new CrashNotificationService(template, configService);
        Map<String, Object> payload = service.buildPayload(event);

        // Verify required fields
        assertNotNull(payload.get("deviceSerial"), "Payload should contain deviceSerial");
        assertNotNull(payload.get("deviceName"), "Payload should contain deviceName");
        assertNotNull(payload.get("crashType"), "Payload should contain crashType");
        assertNotNull(payload.get("packageName"), "Payload should contain packageName");
        assertNotNull(payload.get("severity"), "Payload should contain severity");
        assertNotNull(payload.get("timestamp"), "Payload should contain timestamp");

        assertEquals(event.deviceSerial(), payload.get("deviceSerial"));
        assertEquals(event.crashType(), payload.get("crashType"));
        assertEquals(event.severity().name(), payload.get("severity"));
    }

    @Property(tries = 100)
    void notificationNotSentWhenPreferenceIsNone(@ForAll("crashEvents") CrashEvent event) {
        AlertConfigurationService configService = mock(AlertConfigurationService.class);
        org.springframework.messaging.simp.SimpMessagingTemplate template =
                mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);

        AlertConfigurationEntity config = new AlertConfigurationEntity();
        config.setNotificationPreference(NotificationPreference.NONE);
        when(configService.getForProject(event.projectId())).thenReturn(config);

        CrashNotificationService service = new CrashNotificationService(template, configService);
        service.notifyCrash(event);

        // Verify no messages were sent
        verifyNoInteractions(template);
    }

    @Property(tries = 100)
    void notificationSentWhenPreferenceAllows(
            @ForAll("crashEvents") CrashEvent event,
            @ForAll("enabledPreferences") NotificationPreference pref) {

        AlertConfigurationService configService = mock(AlertConfigurationService.class);
        org.springframework.messaging.simp.SimpMessagingTemplate template =
                mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);

        AlertConfigurationEntity config = new AlertConfigurationEntity();
        config.setNotificationPreference(pref);
        when(configService.getForProject(event.projectId())).thenReturn(config);

        CrashNotificationService service = new CrashNotificationService(template, configService);
        service.notifyCrash(event);

        // Verify messages were sent to the device topic
        verify(template).convertAndSend(
                eq("/topic/crash/" + event.deviceSerial()),
                any(Map.class));
    }

    @Provide
    Arbitrary<NotificationPreference> enabledPreferences() {
        return Arbitraries.of(NotificationPreference.BROWSER, NotificationPreference.IN_APP, NotificationPreference.BOTH);
    }

    @Provide
    Arbitrary<CrashEvent> crashEvents() {
        return Arbitraries.of(
                new CrashEvent("id1", Instant.now(), "device1", "Device One", "com.example.app",
                        "FATAL EXCEPTION", SeverityLevel.FATAL, "trace", null, 1L, 1L),
                new CrashEvent("id2", Instant.now(), "device2", null, "com.test.app",
                        "ANR in", SeverityLevel.ANR, "", null, 2L, 1L),
                new CrashEvent("id3", Instant.now(), "device3", "Device Three", "unknown",
                        "SIGSEGV", SeverityLevel.FATAL, "signal", null, 3L, 2L),
                new CrashEvent("id4", Instant.now(), "device4", "Dev4", "com.app.four",
                        "Process crashed", SeverityLevel.WARNING, "crash", "path/log.log", 4L, 3L)
        );
    }
}
