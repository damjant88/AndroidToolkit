package androidtoolkit.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * REST API on the agent for direct frontend access to device operations.
 * The frontend calls these endpoints directly (localhost:8082) for operations
 * that produce local data (screenshots, screen mirror, recordings).
 * This avoids the WebSocket relay bottleneck through the backend.
 */
@RestController
@RequestMapping("/api/agent/devices")
@CrossOrigin(origins = "*")
public class AgentDeviceController {

    private static final Logger log = LoggerFactory.getLogger(AgentDeviceController.class);
    private final ConcurrentHashMap<String, Process> activeRecordings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Process> activeMirrors = new ConcurrentHashMap<>();

    /**
     * Capture a screenshot and return it directly as PNG bytes.
     * No conversion, no base64 — just raw image data streamed to the client.
     */
    @PostMapping(value = "/{serial}/screenshot", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> captureScreenshot(@PathVariable String serial) {
        try {
            // Capture PNG from device directly to stdout
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "exec-out", "screencap", "-p");
            Process process = pb.start();
            byte[] pngBytes = process.getInputStream().readAllBytes();
            process.waitFor(15, TimeUnit.SECONDS);

            if (pngBytes.length == 0) {
                return ResponseEntity.status(500).build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(pngBytes);
        } catch (Exception e) {
            log.error("Screenshot failed for {}: {}", serial, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Start screen mirror (scrcpy) for a device.
     */
    @PostMapping("/{serial}/screen-mirror")
    public Map<String, Object> startScreenMirror(@PathVariable String serial) {
        try {
            // Stop existing mirror if any
            Process existing = activeMirrors.remove(serial);
            if (existing != null) existing.destroyForcibly();

            ProcessBuilder pb = new ProcessBuilder("scrcpy", "-s", serial, "--no-audio");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeMirrors.put(serial, process);
            return Map.of("success", true, "message", "Screen mirror started for " + serial);
        } catch (Exception e) {
            return Map.of("success", false, "message", "scrcpy failed: " + e.getMessage());
        }
    }

    /**
     * Start screen recording for a device.
     */
    @PostMapping("/{serial}/start-recording")
    public Map<String, Object> startRecording(@PathVariable String serial) {
        if (activeRecordings.containsKey(serial)) {
            return Map.of("success", false, "message", "Recording already in progress");
        }
        try {
            String fileName = "recording_" + serial + "_" + System.currentTimeMillis() + ".mp4";
            String outputPath = "recordings/" + fileName;
            new File("recordings").mkdirs();

            // Use scrcpy for recording (better quality than adb screenrecord)
            ProcessBuilder pb = new ProcessBuilder("scrcpy", "-s", serial, "--no-audio",
                    "--no-display", "--record", outputPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeRecordings.put(serial, process);

            return Map.of("success", true, "message", "Recording started: " + fileName,
                    "fileName", fileName);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Recording failed: " + e.getMessage());
        }
    }

    /**
     * Stop screen recording for a device.
     */
    @PostMapping("/{serial}/stop-recording")
    public Map<String, Object> stopRecording(@PathVariable String serial) {
        Process process = activeRecordings.remove(serial);
        if (process == null) {
            return Map.of("success", false, "message", "No active recording for " + serial);
        }
        process.destroyForcibly();
        return Map.of("success", true, "message", "Recording stopped");
    }

    /**
     * Reboot a device.
     */
    @PostMapping("/{serial}/reboot")
    public Map<String, Object> reboot(@PathVariable String serial) {
        return runSimpleCommand(serial, "reboot", "adb", "-s", serial, "reboot");
    }

    /**
     * Uninstall an app from a device.
     */
    @PostMapping("/{serial}/uninstall")
    public Map<String, Object> uninstall(@PathVariable String serial, @RequestBody Map<String, String> body) {
        String packageName = body.getOrDefault("packageName", "");
        if (packageName.isEmpty()) {
            return Map.of("success", false, "message", "packageName required");
        }
        return runSimpleCommand(serial, "uninstall", "adb", "-s", serial, "uninstall", packageName);
    }

    /**
     * Enable Firebase debug for an app.
     */
    @PostMapping("/{serial}/firebase-debug")
    public Map<String, Object> firebaseDebug(@PathVariable String serial, @RequestBody Map<String, String> body) {
        String packageName = body.getOrDefault("packageName", "");
        return runSimpleCommand(serial, "firebase-debug",
                "adb", "-s", serial, "shell", "setprop", "debug.firebase.analytics.app", packageName);
    }

    /**
     * Pull logs from a device.
     */
    @PostMapping("/{serial}/pull-logs")
    public Map<String, Object> pullLogs(@PathVariable String serial) {
        try {
            String outputDir = "logs/" + serial + "_" + System.currentTimeMillis();
            new File(outputDir).mkdirs();
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "pull", "/sdcard/logs", outputDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            return Map.of("success", exitCode == 0, "message", output.trim(), "path", outputDir);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Toggle WiFi debugging.
     */
    @PostMapping("/{serial}/wifi-debug")
    public Map<String, Object> wifiDebug(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        boolean wifiDebugSession = (boolean) body.getOrDefault("wifiDebugSession", false);
        String ipAddress = (String) body.getOrDefault("ipAddress", "");

        try {
            if (wifiDebugSession) {
                // Disconnect
                runCmd("adb", "disconnect", ipAddress + ":5555");
                return Map.of("success", true, "message", "Disconnected", "wifiDebugSession", false);
            } else if (!ipAddress.isEmpty()) {
                // Connect
                runCmd("adb", "-s", serial, "tcpip", "5555");
                Thread.sleep(1000);
                String result = runCmd("adb", "connect", ipAddress + ":5555");
                boolean connected = result.contains("connected");
                return Map.of("success", connected, "message", result,
                        "wifiDebugSession", connected, "ipAddress", ipAddress);
            }
            return Map.of("success", false, "message", "No WiFi IP available");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private Map<String, Object> runSimpleCommand(String serial, String action, String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            return Map.of("success", exitCode == 0, "message",
                    exitCode == 0 ? action + " completed" : output);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private String runCmd(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor(10, TimeUnit.SECONDS);
        return output;
    }
}
