package androidtoolkit.backend.service;

import androidtoolkit.backend.dto.LogcatData;
import androidtoolkit.domain.DeviceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages per-device logcat streaming processes.
 * Coordinates lifecycle events (start, stop, restart) and tracks stream state
 * for each connected device running SafePath with a valid PID.
 */
@Service
public class LogcatStreamManager {

    private static final Logger log = LoggerFactory.getLogger(LogcatStreamManager.class);
    private static final long RESTART_DELAY_SECONDS = 5;

    private final LogcatParser logcatParser;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, LogcatSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "logcat-retry-scheduler");
                t.setDaemon(true);
                return t;
            }
    );

    public LogcatStreamManager(LogcatParser logcatParser, SimpMessagingTemplate messagingTemplate) {
        this.logcatParser = logcatParser;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Returns the current stream state for the given device serial.
     * Returns {@link StreamState#STOPPED} if no session exists for the serial.
     *
     * @param serial the device serial number
     * @return the current stream state
     */
    public StreamState getStreamState(String serial) {
        LogcatSession session = activeSessions.get(serial);
        if (session == null) {
            return StreamState.STOPPED;
        }
        return session.getState();
    }

    /**
     * Handles device list changes from the DeviceMonitorService.
     * Diffs the current device list against active sessions to:
     * - Start streams for new devices with a valid PID
     * - Stop streams for devices that are no longer present
     * - Restart streams for devices whose PID has changed
     *
     * @param currentDevices the current list of connected devices
     */
    public void onDevicesChanged(List<DeviceInfo> currentDevices) {
        // Build a set of current device serials for quick lookup
        Set<String> currentSerials = new HashSet<>();
        for (DeviceInfo device : currentDevices) {
            currentSerials.add(device.getSerialNumber());
        }

        // Stop streams for devices that are no longer present
        for (String serial : activeSessions.keySet()) {
            if (!currentSerials.contains(serial)) {
                log.info("Device disconnected, stopping logcat stream: {}", serial);
                stopStream(serial);
            }
        }

        // Start or restart streams for current devices
        for (DeviceInfo device : currentDevices) {
            String serial = device.getSerialNumber();
            String pid = device.getPid();

            // Skip devices without a valid PID
            if (pid == null || pid.isEmpty()) {
                continue;
            }

            LogcatSession existingSession = activeSessions.get(serial);

            if (existingSession == null) {
                // New device with valid PID — start stream
                log.info("New device discovered, starting logcat stream: {} (PID: {})", serial, pid);
                startStream(serial, pid);
            } else if (!Objects.equals(existingSession.getPid(), pid)) {
                // PID changed — stop existing stream and start new one
                log.info("PID changed for device {}: {} -> {}, restarting logcat stream",
                        serial, existingSession.getPid(), pid);
                stopStream(serial);
                startStream(serial, pid);
            }
            // If device exists with same PID, do nothing — stream is already running
        }
    }

    /**
     * Starts a logcat stream for the given device serial and PID.
     * Launches an adb logcat process and creates a virtual thread to read its output.
     * Ensures at most one process per device by stopping any existing stream first.
     *
     * @param serial the device serial number
     * @param pid the SafePath app process ID
     */
    public void startStream(String serial, String pid) {
        // Ensure at most one process per device
        if (activeSessions.containsKey(serial)) {
            stopStream(serial);
        }

        LogcatSession session = new LogcatSession(serial, pid);
        activeSessions.put(serial, session);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "adb", "-s", serial, "logcat", "-s", "OkHttp:I", "--pid=" + pid
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            session.setProcess(process);
            session.setState(StreamState.RUNNING);

            // Create a virtual thread to read stdout line-by-line
            Thread readerThread = Thread.ofVirtual().name("logcat-reader-" + serial).start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Optional<ParsedField> parsed = logcatParser.parseLine(line);
                        if (parsed.isPresent()) {
                            log.info("Parsed {} = {} for device {}", parsed.get().type(), parsed.get().value(), serial);
                            updateAndBroadcast(session, parsed.get());
                        }
                    }
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        log.warn("Error reading logcat stream for device {}: {}", serial, e.getMessage());
                    }
                }

                // EOF reached — check if this was an unexpected exit
                handleUnexpectedExit(session);
            });
            session.setReaderThread(readerThread);
        } catch (IOException e) {
            log.warn("Failed to start logcat process for device {} (PID: {}): {}", serial, pid, e.getMessage());
            session.setState(StreamState.ERRORED);
        }
    }

    /**
     * Stops the logcat stream for the given device serial.
     * Terminates the process, interrupts the reader thread, sets state to STOPPED,
     * and removes the session from the active sessions map.
     *
     * @param serial the device serial number
     */
    public void stopStream(String serial) {
        LogcatSession session = activeSessions.remove(serial);
        if (session != null) {
            terminateSession(session);
        }
    }

    /**
     * Terminates all active logcat processes and clears all sessions.
     * Called on application shutdown to release resources.
     */
    @PreDestroy
    public void shutdown() {
        retryScheduler.shutdownNow();
        for (LogcatSession session : activeSessions.values()) {
            terminateSession(session);
        }
        activeSessions.clear();
    }

    /**
     * Terminates the process and interrupts the reader thread for a session.
     */
    private void terminateSession(LogcatSession session) {
        Process process = session.getProcess();
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        Thread readerThread = session.getReaderThread();
        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
        }
        session.setState(StreamState.STOPPED);
    }

    /**
     * Handles unexpected process exit (EOF while state is still RUNNING).
     * Implements retry logic: one restart attempt after a 5-second delay.
     * If the first restart also fails, marks the stream as ERRORED.
     */
    private void handleUnexpectedExit(LogcatSession session) {
        if (session.getState() != StreamState.RUNNING) {
            // Process was intentionally stopped — no retry needed
            return;
        }

        String serial = session.getSerial();
        String pid = session.getPid();
        int attempts = session.getRestartAttempts();

        if (attempts == 0) {
            // First unexpected exit — schedule a restart after 5-second delay
            log.warn("Logcat process for device {} exited unexpectedly, scheduling restart in {}s",
                    serial, RESTART_DELAY_SECONDS);
            session.setRestartAttempts(1);
            retryScheduler.schedule(() -> {
                // Only restart if the session is still in the map and still for the same device/PID
                LogcatSession currentSession = activeSessions.get(serial);
                if (currentSession == session && currentSession.getState() == StreamState.RUNNING) {
                    log.info("Retrying logcat stream for device {} (PID: {})", serial, pid);
                    // Remove old session and start fresh, preserving restart attempts
                    activeSessions.remove(serial);
                    startStream(serial, pid);
                    // Transfer restart attempts to the new session
                    LogcatSession newSession = activeSessions.get(serial);
                    if (newSession != null) {
                        newSession.setRestartAttempts(1);
                    }
                }
            }, RESTART_DELAY_SECONDS, TimeUnit.SECONDS);
        } else {
            // Second unexpected exit — mark as ERRORED, no further retries
            log.error("Logcat process for device {} failed after restart attempt, marking as ERRORED", serial);
            session.setState(StreamState.ERRORED);
        }
    }

    /**
     * Updates the LogcatData for a session with a parsed field and broadcasts if changed.
     */
    private void updateAndBroadcast(LogcatSession session, ParsedField field) {
        LogcatData data = session.getCurrentData();
        boolean changed = false;

        switch (field.type()) {
            case ENVIRONMENT -> {
                if (!Objects.equals(data.getEnvironment(), field.value())) {
                    data.setEnvironment(field.value());
                    changed = true;
                }
            }
            case CLIENT_VERSION -> {
                if (!Objects.equals(data.getClientVersion(), field.value())) {
                    data.setClientVersion(field.value());
                    changed = true;
                }
            }
            case SERVER_PRODUCT_VERSION -> {
                if (!Objects.equals(data.getServerProductVersion(), field.value())) {
                    data.setServerProductVersion(field.value());
                    changed = true;
                }
            }
            case ACCESS_TOKEN -> {
                if (!Objects.equals(data.getAccessToken(), field.value())) {
                    data.setAccessToken(field.value());
                    changed = true;
                }
            }
        }

        if (changed) {
            messagingTemplate.convertAndSend("/topic/logcat/" + session.getSerial(), data);
        }
    }

    // Package-private accessor for testing
    ConcurrentHashMap<String, LogcatSession> getActiveSessions() {
        return activeSessions;
    }

    /**
     * Returns the current LogcatData for the given device serial, or null if no session exists.
     */
    public LogcatData getCurrentData(String serial) {
        LogcatSession session = activeSessions.get(serial);
        return session != null ? session.getCurrentData() : null;
    }
}
