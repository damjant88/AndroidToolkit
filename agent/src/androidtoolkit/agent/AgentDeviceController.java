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
    private final ConcurrentHashMap<String, Map<String, String>> recordingMeta = new ConcurrentHashMap<>();

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
     * This starts BOTH scrcpy (screen mirror with display) AND adb screenrecord on the device.
     * Also captures the app PID for log filtering when recording stops.
     */
    @PostMapping("/{serial}/start-recording")
    public Map<String, Object> startRecording(@PathVariable String serial,
                                              @RequestBody(required = false) Map<String, String> body) {
        if (activeRecordings.containsKey(serial)) {
            return Map.of("success", false, "message", "Recording already in progress");
        }
        try {
            String fileName = "screen_record_" + System.currentTimeMillis() + ".mp4";

            // Get the app PID for log filtering later
            String pid = runCmd("adb", "-s", serial, "shell", "pidof", "-s",
                    body != null ? body.getOrDefault("packageName", "") : "");

            // Start scrcpy for live screen mirror (with display)
            ProcessBuilder scrcpyPb = new ProcessBuilder("scrcpy", "-s", serial, "--no-audio");
            scrcpyPb.redirectErrorStream(true);
            Process scrcpyProcess = scrcpyPb.start();
            activeMirrors.put(serial, scrcpyProcess);

            // Start adb screenrecord on the device (records to /sdcard/)
            ProcessBuilder recordPb = new ProcessBuilder("adb", "-s", serial, "shell",
                    "screenrecord", "--bit-rate", "4000000", "/sdcard/" + fileName);
            recordPb.redirectErrorStream(true);
            Process recordProcess = recordPb.start();
            activeRecordings.put(serial, recordProcess);

            // Store metadata for stop
            recordingMeta.put(serial, Map.of("fileName", fileName, "pid", pid.trim()));

            return Map.of("success", true, "message", "Recording started: " + fileName);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Recording failed: " + e.getMessage());
        }
    }

    /**
     * Stop screen recording for a device.
     * Stops screenrecord, pulls the video from device, captures logcat, saves both.
     */
    @PostMapping("/{serial}/stop-recording")
    public Map<String, Object> stopRecording(@PathVariable String serial) {
        Process recordProcess = activeRecordings.remove(serial);
        Process mirrorProcess = activeMirrors.remove(serial);
        Map<String, String> meta = recordingMeta.remove(serial);

        if (recordProcess == null) {
            return Map.of("success", false, "message", "No active recording for " + serial);
        }

        try {
            // Stop screenrecord gracefully via SIGINT
            runCmd("adb", "-s", serial, "shell", "pkill", "-INT", "screenrecord");
            recordProcess.waitFor(10, TimeUnit.SECONDS);
            if (recordProcess.isAlive()) recordProcess.destroyForcibly();

            // Stop scrcpy
            if (mirrorProcess != null) mirrorProcess.destroyForcibly();

            String fileName = meta != null ? meta.getOrDefault("fileName", "recording.mp4") : "recording.mp4";
            String pid = meta != null ? meta.getOrDefault("pid", "") : "";

            // Create output directory
            String dateStr = java.time.LocalDate.now().toString();
            String outputDir = "recordings/" + serial + "/" + dateStr;
            new File(outputDir).mkdirs();

            // Pull video from device
            runCmd("adb", "-s", serial, "pull", "/sdcard/" + fileName, outputDir + "/" + fileName);
            runCmd("adb", "-s", serial, "shell", "rm", "/sdcard/" + fileName);

            // Capture logcat (filtered by PID if available)
            String logFile = outputDir + "/" + fileName + ".log";
            if (!pid.isEmpty()) {
                runCmdAndSave("adb -s " + serial + " logcat -d --pid=" + pid, logFile);
            } else {
                runCmdAndSave("adb -s " + serial + " logcat -d", logFile);
            }

            String absPath = new File(outputDir).getAbsolutePath();
            return Map.of("success", true, "message", "Recording saved",
                    "recordingLocation", absPath, "recordingFileName", fileName);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Stop recording failed: " + e.getMessage());
        }
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

    /**
     * Receive an APK file and save it locally for installation.
     * The APK is NOT uploaded to the backend — it stays on the agent machine.
     */
    @PostMapping("/upload-apk")
    public Map<String, Object> uploadApk(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String apksDir = "apks";
            new File(apksDir).mkdirs();
            String filePath = apksDir + "/" + file.getOriginalFilename();
            file.transferTo(new File(filePath).getAbsoluteFile());
            return Map.of("success", true, "path", new File(filePath).getAbsolutePath(),
                    "fileName", file.getOriginalFilename());
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to save APK: " + e.getMessage());
        }
    }

    /**
     * Install an APK on a device from a local path on the agent machine.
     */
    @PostMapping("/{serial}/install")
    public Map<String, Object> installApk(@PathVariable String serial, @RequestBody Map<String, String> body) {
        String apkPath = body.getOrDefault("apkPath", "");
        if (apkPath.isEmpty()) {
            return Map.of("success", false, "message", "apkPath required");
        }
        // If path is not absolute, look in the apks/ directory
        File apkFile = new File(apkPath);
        if (!apkFile.isAbsolute() || !apkFile.exists()) {
            apkFile = new File("apks", apkPath);
        }
        if (!apkFile.exists()) {
            return Map.of("success", false, "message", "APK not found: " + apkPath + " (also checked apks/" + apkPath + ")");
        }
        return runSimpleCommand(serial, "install", "adb", "-s", serial, "install", "-r", apkFile.getAbsolutePath());
    }

    /**
     * Open a folder in the system file explorer.
     */
    @PostMapping("/open-folder")
    public Map<String, Object> openFolder(@RequestParam("path") String folderPath) {
        try {
            File folder = new File(folderPath).getAbsoluteFile();
            if (!folder.exists()) folder = folder.getParentFile();
            if (folder == null || !folder.exists()) {
                return Map.of("success", false, "message", "Folder not found: " + folderPath);
            }
            new ProcessBuilder("explorer.exe", folder.getAbsolutePath()).start();
            return Map.of("success", true, "message", "Opened: " + folder.getAbsolutePath());
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed: " + e.getMessage());
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

    private void runCmdAndSave(String command, String outputFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command + " > \"" + outputFile + "\"");
            // On Windows, use cmd /c instead
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", command + " > \"" + outputFile + "\"");
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Fallback: run command and write output manually
            try {
                String[] parts = command.split("\\s+");
                ProcessBuilder fallback = new ProcessBuilder(parts);
                fallback.redirectErrorStream(true);
                Process process = fallback.start();
                byte[] output = process.getInputStream().readAllBytes();
                process.waitFor(30, TimeUnit.SECONDS);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(output);
                }
            } catch (Exception ex) {
                log.warn("Failed to save command output to {}: {}", outputFile, ex.getMessage());
            }
        }
    }
}
