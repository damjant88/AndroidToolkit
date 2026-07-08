package androidtoolkit.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.List;
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
    private final DeviceDiscoveryScheduler deviceDiscoveryScheduler;
    private final AgentLogcatService logcatService;

    public AgentDeviceController(DeviceDiscoveryScheduler deviceDiscoveryScheduler, AgentLogcatService logcatService) {
        this.deviceDiscoveryScheduler = deviceDiscoveryScheduler;
        this.logcatService = logcatService;
    }

    /**
     * Triggers an immediate device info refresh so the backend/frontend
     * gets updated package and PID info after install/uninstall.
     */
    private void triggerDeviceRefresh() {
        try {
            // Small delay to let the package manager settle
            Thread.sleep(1000);
            deviceDiscoveryScheduler.refreshDeviceInfo();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Capture a screenshot and return it directly as PNG bytes.
     * No conversion, no base64 — just raw image data streamed to the client.
     */
    @PostMapping(value = "/{serial}/screenshot", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> captureScreenshot(@PathVariable String serial) {
        try {
            // Retry up to 3 times with 500ms delay if screencap returns empty
            byte[] pngBytes = null;
            for (int attempt = 0; attempt < 3; attempt++) {
                ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "exec-out", "screencap", "-p");
                Process process = pb.start();
                pngBytes = process.getInputStream().readAllBytes();
                process.waitFor(15, TimeUnit.SECONDS);
                if (pngBytes.length > 0) break;
                Thread.sleep(500);
            }

            if (pngBytes == null || pngBytes.length == 0) {
                return ResponseEntity.status(500).body(null);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(pngBytes);
        } catch (Exception e) {
            log.error("Screenshot failed for {}: {}", serial, e.getMessage());
            return ResponseEntity.status(500).body(null);
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
     * If packageName is empty, auto-detects the installed SafePath package.
     */
    @PostMapping("/{serial}/uninstall")
    public Map<String, Object> uninstall(@PathVariable String serial, @RequestBody Map<String, String> body) {
        String packageName = body.getOrDefault("packageName", "");
        if (packageName.isEmpty()) {
            // Auto-detect the package to uninstall
            packageName = detectPackage(serial);
        }
        if (packageName.isEmpty()) {
            return Map.of("success", false, "message", "No supported package found on device");
        }
        // Clear app data, disable for user, then uninstall (matches Swing desktop behavior)
        runSimpleCommand(serial, "clear", "adb", "-s", serial, "shell", "pm", "clear", packageName);
        runSimpleCommand(serial, "disable", "adb", "-s", serial, "shell", "pm", "disable-user", "--user", "0", packageName);
        Map<String, Object> result = runSimpleCommand(serial, "uninstall", "adb", "-s", serial, "shell", "pm", "uninstall", packageName);
        triggerDeviceRefresh();
        return result;
    }

    private String detectPackage(String serial) {
        try {
            String output = runCmd("adb", "-s", serial, "shell", "pm", "list", "packages");
            List<String> supported = List.of(
                    "com.smithmicro.tmobile.familymode.test", "com.smithmicro.att.securefamily",
                    "com.att.securefamilycompanion", "com.wavemarket.waplauncher",
                    "com.smithmicro.safepath.family", "com.smithmicro.safepath.family.light",
                    "com.smithmicro.safepath.family.speakeasy", "com.smithmicro.cci.test",
                    "com.smithmicro.sprint.safeandfound.test", "com.sprint.safefound",
                    "com.tmobile.familycontrols", "com.smithmicro.orangespain.test",
                    "com.orange.es.TuYo", "com.smithmicro.safepath.dish.test",
                    "com.smithmicro.safepath.dish.kid.test", "com.smithmicro.safepath.family.child");
            for (String line : output.split("\n")) {
                String pkg = line.replace("package:", "").trim();
                if (supported.contains(pkg)) return pkg;
            }
        } catch (Exception e) { /* ignore */ }
        return "";
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
     * Pull logs from a device — captures logcat, app logs, and opens the folder.
     */
    @PostMapping("/{serial}/pull-logs")
    public Map<String, Object> pullLogs(@PathVariable String serial, @RequestBody(required = false) Map<String, String> body) {
        try {
            String logFolder = (body != null) ? body.getOrDefault("logFolder", "") : "";
            String packageName = detectPackage(serial);
            String deviceModel = runCmd("adb", "-s", serial, "shell", "getprop", "ro.product.model").trim().replace(' ', '_');
            String date = java.time.LocalDate.now().toString();
            String safeSerial = serial.replace(":", "-").replace(".", "_");

            // Use user-configured log folder if provided, otherwise default
            String baseDir = (logFolder != null && !logFolder.isBlank()) ? logFolder : "logs";
            String outputDir = baseDir + "/" + deviceModel + "_" + safeSerial + "/" + date;
            new File(outputDir).mkdirs();

            // 1. Capture logcat (full device log)
            String logcatFile = outputDir + "/" + deviceModel + "_" + safeSerial + "_logcat_" + date + ".log";
            runCmdAndSave("adb -s " + serial + " logcat -d", logcatFile);

            // 2. If app is installed, capture PID-filtered logcat
            if (!packageName.isEmpty()) {
                String pid = runCmd("adb", "-s", serial, "shell", "pidof", "-s", packageName).trim();
                if (!pid.isEmpty()) {
                    String appLogFile = outputDir + "/" + deviceModel + "_" + safeSerial + "_app_" + date + ".log";
                    runCmdAndSave("adb -s " + serial + " logcat -d --pid=" + pid, appLogFile);
                }
            }

            // 3. Pull app-specific logs from device storage
            runCmd("adb", "-s", serial, "pull", "/sdcard/Android/data/" + packageName + "/files/logs", outputDir);
            runCmd("adb", "-s", serial, "pull", "/sdcard/logs", outputDir);

            String absPath = new File(outputDir).getAbsolutePath();

            // 4. Open the folder
            try {
                new ProcessBuilder("explorer.exe", absPath).start();
            } catch (Exception e) { /* ignore if not on Windows */ }

            return Map.of("success", true, "message", "Logs saved to " + absPath,
                    "exportedLogsFolder", absPath, "selectedFolder", absPath);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Pull logs failed: " + e.getMessage());
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
                // Disconnect — fast, no need to suppress
                ProcessBuilder pb = new ProcessBuilder("adb", "disconnect", ipAddress + ":5555");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.getInputStream().readAllBytes();
                process.waitFor(3, TimeUnit.SECONDS);
                if (process.isAlive()) process.destroyForcibly();
                return Map.of("success", true, "message", "Disconnected", "wifiDebugSession", false);
            } else if (!ipAddress.isEmpty()) {
                // Connect — suppress device list updates during toggle
                deviceDiscoveryScheduler.suppressUpdates(10000);
                runCmd("adb", "-s", serial, "tcpip", "5555");
                Thread.sleep(2000);
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
    public Map<String, Object> uploadApk(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                         @RequestParam(value = "targetFolder", required = false) String targetFolder) {
        try {
            String apksDir = (targetFolder != null && !targetFolder.isBlank()) ? targetFolder : "apks";
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
        Map<String, Object> result = runSimpleCommand(serial, "install", "adb", "-s", serial, "install", "-r", apkFile.getAbsolutePath());
        // Clear stale logcat metadata from previous build
        logcatService.clearMetadata(serial);
        // Trigger immediate device info refresh so the UI updates with new package info
        triggerDeviceRefresh();
        return result;
    }

    /**
     * Get parsed logcat metadata for a device (environment, version, token, etc.)
     */
    @GetMapping("/{serial}/logcat-data")
    public Map<String, Object> getLogcatData(@PathVariable String serial) {
        AgentLogcatService.LogcatMetadata meta = logcatService.getMetadata(serial);
        return Map.of(
                "serial", serial,
                "environment", meta.environment != null ? meta.environment : "",
                "clientVersion", meta.clientVersion != null ? meta.clientVersion : "",
                "serverProductVersion", meta.serverProductVersion != null ? meta.serverProductVersion : "",
                "serverProjectVersion", meta.serverProjectVersion != null ? meta.serverProjectVersion : "",
                "accessToken", meta.accessToken != null ? meta.accessToken : "",
                "tokenType", meta.tokenType != null ? meta.tokenType : ""
        );
    }

    /**
     * Start logcat monitoring for a device.
     */
    @PostMapping("/{serial}/start-logcat")
    public Map<String, Object> startLogcat(@PathVariable String serial, @RequestBody(required = false) Map<String, String> body) {
        String pid = body != null ? body.getOrDefault("pid", "") : "";
        logcatService.startMonitoring(serial, pid);
        return Map.of("success", true, "message", "Logcat monitoring started");
    }

    /**
     * Stop logcat monitoring for a device.
     */
    @PostMapping("/{serial}/stop-logcat")
    public Map<String, Object> stopLogcat(@PathVariable String serial) {
        logcatService.stopMonitoring(serial);
        return Map.of("success", true, "message", "Logcat monitoring stopped");
    }

    /**
     * Get the device's last known GPS location.
     */
    @GetMapping("/{serial}/location")
    public Map<String, Object> getLocation(@PathVariable String serial) {
        try {
            String output = runCmd("adb", "-s", serial, "shell", "dumpsys", "location");
            double lat = 0, lng = 0;
            boolean found = false;
            for (String line : output.split("\n")) {
                if (line.contains("last location=") && line.contains("Location[")) {
                    try {
                        int idx = line.indexOf("Location[");
                        String sub = line.substring(idx);
                        int start = sub.indexOf(' ') + 1;
                        int comma = sub.indexOf(',', start);
                        int end = sub.indexOf(' ', comma);
                        if (end == -1) end = sub.indexOf(']', comma);
                        lat = Double.parseDouble(sub.substring(start, comma));
                        lng = Double.parseDouble(sub.substring(comma + 1, end));
                        found = true;
                        break;
                    } catch (Exception ignored) {}
                }
            }
            return Map.of("success", found, "lat", lat, "lng", lng);
        } catch (Exception e) {
            return Map.of("success", false, "lat", 0.0, "lng", 0.0, "message", e.getMessage());
        }
    }

    /**
     * Set or stop mock GPS location on a device.
     */
    @PostMapping("/{serial}/mock-location")
    public Map<String, Object> mockLocation(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        double lat = ((Number) body.getOrDefault("lat", 0.0)).doubleValue();
        double lng = ((Number) body.getOrDefault("lng", 0.0)).doubleValue();
        boolean start = (boolean) body.getOrDefault("start", true);

        try {
            if (!start) {
                runCmd("adb", "-s", serial, "shell", "cmd", "location", "providers", "remove-test-provider", "gps");
                return Map.of("success", true, "message", "Mock location stopped");
            }
            runCmd("adb", "-s", serial, "shell", "appops", "set", "com.android.shell", "android:mock_location", "allow");
            runCmd("adb", "-s", serial, "shell", "cmd", "location", "providers", "add-test-provider", "gps");
            runCmd("adb", "-s", serial, "shell", "cmd", "location", "providers", "set-test-provider-enabled", "gps", "true");
            String locCmd = String.format("cmd location providers set-test-provider-location gps --location %f,%f --accuracy 1.0", lat, lng);
            runCmd("adb", "-s", serial, "shell", locCmd);
            return Map.of("success", true, "message", String.format("Mocking: %.6f,%.6f", lat, lng));
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private final ConcurrentHashMap<String, Process> activeDownloads = new ConcurrentHashMap<>();

    /**
     * Download an APK from S3 using aws s3 cp into the target folder.
     * Streams progress output back to the caller.
     */
    @PostMapping("/download-s3")
    public Map<String, Object> downloadFromS3(@RequestBody Map<String, String> body) {
        String s3Path = body.getOrDefault("s3Path", "");
        String targetFolder = body.getOrDefault("targetFolder", "apks");
        String downloadId = body.getOrDefault("downloadId", String.valueOf(System.currentTimeMillis()));
        if (s3Path.isEmpty()) {
            return Map.of("success", false, "message", "s3Path is required");
        }
        try {
            new File(targetFolder).mkdirs();
            String fileName = s3Path.substring(s3Path.lastIndexOf('/') + 1);
            String localPath = targetFolder + "/" + fileName;
            ProcessBuilder pb = new ProcessBuilder("aws", "s3", "cp", s3Path, localPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeDownloads.put(downloadId, process);

            // Read output line by line to capture progress
            StringBuilder output = new StringBuilder();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            boolean success = process.waitFor(120, TimeUnit.SECONDS) && process.exitValue() == 0;
            if (!success && process.isAlive()) process.destroyForcibly();
            activeDownloads.remove(downloadId);

            String outputStr = output.toString().trim();
            return Map.of("success", success,
                    "message", success ? "Downloaded: " + fileName : outputStr,
                    "localPath", new File(localPath).getAbsolutePath(),
                    "fileName", fileName,
                    "output", outputStr,
                    "targetFolder", new File(targetFolder).getAbsolutePath());
        } catch (Exception e) {
            activeDownloads.remove(downloadId);
            return Map.of("success", false, "message", "Download failed: " + e.getMessage());
        }
    }

    /**
     * Cancel an active S3 download.
     */
    @PostMapping("/cancel-download")
    public Map<String, Object> cancelDownload(@RequestBody Map<String, String> body) {
        String downloadId = body.getOrDefault("downloadId", "");
        Process process = activeDownloads.remove(downloadId);
        if (process != null) {
            process.destroyForcibly();
            return Map.of("success", true, "message", "Download cancelled");
        }
        return Map.of("success", false, "message", "No active download found");
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
