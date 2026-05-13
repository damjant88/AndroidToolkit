package androidtoolkit.backend.service;

import androidtoolkit.domain.DeviceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogcatStreamManagerTest {

    private LogcatStreamManager manager;
    private SimpMessagingTemplate messagingTemplate;
    private LogcatParser logcatParser;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        logcatParser = new LogcatParser();
        manager = new LogcatStreamManager(logcatParser, messagingTemplate);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private DeviceInfo device(String serial, String pid) {
        return new DeviceInfo(serial, "Samsung", "Galaxy S21", "14", "", "", "192.168.1.1", "com.safepath", true, pid);
    }

    // ─── getStreamState ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStreamState")
    class GetStreamStateTests {

        @Test
        @DisplayName("Returns STOPPED for unknown serial")
        void returnsStoppedForUnknownSerial() {
            assertEquals(StreamState.STOPPED, manager.getStreamState("unknown-serial"));
        }

        @Test
        @DisplayName("Returns ERRORED after startStream with invalid serial (adb not found)")
        void returnsErroredAfterFailedStart() {
            // Using a fake serial will cause the adb process to fail to launch
            // (adb binary may not exist or the serial is invalid)
            manager.startStream("FAKE_SERIAL_12345", "9999");

            // Give a moment for the process to attempt launch
            StreamState state = manager.getStreamState("FAKE_SERIAL_12345");
            // The process either errors immediately (ERRORED) or starts and quickly exits
            // On systems without adb, it will be ERRORED due to IOException
            // On systems with adb, it may be RUNNING briefly then exit
            assertTrue(state == StreamState.ERRORED || state == StreamState.RUNNING,
                    "Expected ERRORED or RUNNING, got: " + state);
        }
    }

    // ─── startStream ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startStream")
    class StartStreamTests {

        @Test
        @DisplayName("Creates a session in activeSessions map")
        void createsSession() {
            manager.startStream("DEVICE_A", "1234");

            ConcurrentHashMap<String, LogcatSession> sessions = manager.getActiveSessions();
            assertTrue(sessions.containsKey("DEVICE_A"));
        }

        @Test
        @DisplayName("Session has correct serial and PID")
        void sessionHasCorrectSerialAndPid() {
            manager.startStream("DEVICE_B", "5678");

            LogcatSession session = manager.getActiveSessions().get("DEVICE_B");
            assertNotNull(session);
            assertEquals("DEVICE_B", session.getSerial());
            assertEquals("5678", session.getPid());
        }

        @Test
        @DisplayName("Replaces existing session for same serial")
        void replacesExistingSession() {
            manager.startStream("DEVICE_C", "1111");
            manager.startStream("DEVICE_C", "2222");

            ConcurrentHashMap<String, LogcatSession> sessions = manager.getActiveSessions();
            assertEquals(1, sessions.size());
            assertEquals("2222", sessions.get("DEVICE_C").getPid());
        }
    }

    // ─── stopStream ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("stopStream")
    class StopStreamTests {

        @Test
        @DisplayName("No-op for non-existent serial")
        void noOpForNonExistentSerial() {
            // Should not throw
            assertDoesNotThrow(() -> manager.stopStream("non-existent-serial"));
        }

        @Test
        @DisplayName("Removes session from activeSessions")
        void removesSession() {
            manager.startStream("DEVICE_D", "3333");
            assertTrue(manager.getActiveSessions().containsKey("DEVICE_D"));

            manager.stopStream("DEVICE_D");
            assertFalse(manager.getActiveSessions().containsKey("DEVICE_D"));
        }

        @Test
        @DisplayName("State returns STOPPED after stopStream")
        void stateIsStoppedAfterStop() {
            manager.startStream("DEVICE_E", "4444");
            manager.stopStream("DEVICE_E");

            assertEquals(StreamState.STOPPED, manager.getStreamState("DEVICE_E"));
        }
    }

    // ─── onDevicesChanged ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onDevicesChanged")
    class OnDevicesChangedTests {

        @Test
        @DisplayName("Starts streams for new devices with valid PID")
        void startsStreamsForNewDevices() {
            List<DeviceInfo> devices = List.of(
                    device("SERIAL_1", "100"),
                    device("SERIAL_2", "200")
            );

            manager.onDevicesChanged(devices);

            ConcurrentHashMap<String, LogcatSession> sessions = manager.getActiveSessions();
            assertTrue(sessions.containsKey("SERIAL_1"));
            assertTrue(sessions.containsKey("SERIAL_2"));
        }

        @Test
        @DisplayName("Stops streams for removed devices")
        void stopsStreamsForRemovedDevices() {
            // First, add two devices
            manager.onDevicesChanged(List.of(
                    device("SERIAL_A", "100"),
                    device("SERIAL_B", "200")
            ));
            assertTrue(manager.getActiveSessions().containsKey("SERIAL_A"));
            assertTrue(manager.getActiveSessions().containsKey("SERIAL_B"));

            // Now only SERIAL_A is present — SERIAL_B should be stopped
            manager.onDevicesChanged(List.of(device("SERIAL_A", "100")));

            assertTrue(manager.getActiveSessions().containsKey("SERIAL_A"));
            assertFalse(manager.getActiveSessions().containsKey("SERIAL_B"));
        }

        @Test
        @DisplayName("Restarts stream on PID change")
        void restartsOnPidChange() {
            manager.onDevicesChanged(List.of(device("SERIAL_X", "100")));
            LogcatSession originalSession = manager.getActiveSessions().get("SERIAL_X");
            assertEquals("100", originalSession.getPid());

            // PID changes to 200
            manager.onDevicesChanged(List.of(device("SERIAL_X", "200")));
            LogcatSession newSession = manager.getActiveSessions().get("SERIAL_X");
            assertNotNull(newSession);
            assertEquals("200", newSession.getPid());
        }

        @Test
        @DisplayName("Skips devices with null PID")
        void skipsDevicesWithNullPid() {
            manager.onDevicesChanged(List.of(device("SERIAL_NULL", null)));

            assertFalse(manager.getActiveSessions().containsKey("SERIAL_NULL"));
        }

        @Test
        @DisplayName("Skips devices with empty PID")
        void skipsDevicesWithEmptyPid() {
            manager.onDevicesChanged(List.of(device("SERIAL_EMPTY", "")));

            assertFalse(manager.getActiveSessions().containsKey("SERIAL_EMPTY"));
        }

        @Test
        @DisplayName("Does not restart when PID is unchanged")
        void doesNotRestartWhenPidUnchanged() {
            manager.onDevicesChanged(List.of(device("SERIAL_SAME", "500")));
            LogcatSession firstSession = manager.getActiveSessions().get("SERIAL_SAME");

            // Same device, same PID — session should remain the same object
            manager.onDevicesChanged(List.of(device("SERIAL_SAME", "500")));
            LogcatSession secondSession = manager.getActiveSessions().get("SERIAL_SAME");

            assertSame(firstSession, secondSession);
        }

        @Test
        @DisplayName("Handles empty device list — stops all streams")
        void handlesEmptyDeviceList() {
            manager.onDevicesChanged(List.of(
                    device("DEV_1", "10"),
                    device("DEV_2", "20")
            ));
            assertEquals(2, manager.getActiveSessions().size());

            manager.onDevicesChanged(List.of());
            assertTrue(manager.getActiveSessions().isEmpty());
        }
    }

    // ─── shutdown ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("shutdown")
    class ShutdownTests {

        @Test
        @DisplayName("Clears all active sessions")
        void clearsAllSessions() {
            manager.startStream("S1", "111");
            manager.startStream("S2", "222");
            manager.startStream("S3", "333");
            assertEquals(3, manager.getActiveSessions().size());

            manager.shutdown();

            assertTrue(manager.getActiveSessions().isEmpty());
        }

        @Test
        @DisplayName("getStreamState returns STOPPED after shutdown")
        void stateIsStoppedAfterShutdown() {
            manager.startStream("S4", "444");
            manager.shutdown();

            assertEquals(StreamState.STOPPED, manager.getStreamState("S4"));
        }
    }

    // ─── startTracking / stopTracking ────────────────────────────────────────────

    @Nested
    @DisplayName("startTracking / stopTracking")
    class TrackingTests {

        @Test
        @DisplayName("startTracking sets keyword on session")
        void startTrackingSetsKeyword() {
            manager.startStream("TRACK_DEV", "777");

            manager.startTracking("TRACK_DEV", "NullPointerException");

            LogcatSession session = manager.getActiveSessions().get("TRACK_DEV");
            assertEquals("NullPointerException", session.getTrackingKeyword());
        }

        @Test
        @DisplayName("stopTracking clears keyword on session")
        void stopTrackingClearsKeyword() {
            manager.startStream("TRACK_DEV2", "888");
            manager.startTracking("TRACK_DEV2", "OutOfMemory");

            manager.stopTracking("TRACK_DEV2");

            LogcatSession session = manager.getActiveSessions().get("TRACK_DEV2");
            assertNull(session.getTrackingKeyword());
        }

        @Test
        @DisplayName("startTracking on non-existent session is a no-op")
        void startTrackingNoOpForMissingSession() {
            // Should not throw
            assertDoesNotThrow(() -> manager.startTracking("NO_SUCH_DEVICE", "keyword"));
        }

        @Test
        @DisplayName("stopTracking on non-existent session is a no-op")
        void stopTrackingNoOpForMissingSession() {
            // Should not throw
            assertDoesNotThrow(() -> manager.stopTracking("NO_SUCH_DEVICE"));
        }

        @Test
        @DisplayName("startTracking resets line buffer and afterMatchCount")
        void startTrackingResetsState() {
            manager.startStream("TRACK_DEV3", "999");
            LogcatSession session = manager.getActiveSessions().get("TRACK_DEV3");

            // Simulate some prior state
            session.getLineBuffer().add("old line");
            session.setAfterMatchCount(3);

            manager.startTracking("TRACK_DEV3", "NewKeyword");

            assertEquals("NewKeyword", session.getTrackingKeyword());
            assertTrue(session.getLineBuffer().isEmpty());
            assertEquals(-1, session.getAfterMatchCount());
        }
    }

    // ─── getCurrentData ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCurrentData")
    class GetCurrentDataTests {

        @Test
        @DisplayName("Returns null for non-existent session")
        void returnsNullForMissingSession() {
            assertNull(manager.getCurrentData("NO_DEVICE"));
        }

        @Test
        @DisplayName("Returns LogcatData with correct serial")
        void returnsDataWithCorrectSerial() {
            manager.startStream("DATA_DEV", "555");

            var data = manager.getCurrentData("DATA_DEV");
            assertNotNull(data);
            assertEquals("DATA_DEV", data.getSerial());
        }
    }
}
