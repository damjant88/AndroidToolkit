package androidtoolkit.backend.controller;

import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.ScreenshotCaptureResponse;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;
import androidtoolkit.backend.device.DeviceProvider;
import androidtoolkit.backend.dto.LogcatData;
import androidtoolkit.backend.security.TenantContext;
import androidtoolkit.backend.service.LogcatStreamManager;
import androidtoolkit.service.CommandExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static androidtoolkit.backend.validation.InputValidator.validatePackageName;
import static androidtoolkit.backend.validation.InputValidator.validateSerial;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceProvider deviceProvider;
    private final CommandExecutor commandExecutor;
    private final LogcatStreamManager logcatStreamManager;
    private final ConcurrentHashMap<String, Process> activeMocks = new ConcurrentHashMap<>();

    public DeviceController(
            DeviceProvider deviceProvider,
            CommandExecutor commandExecutor,
            LogcatStreamManager logcatStreamManager
    ) {
        this.deviceProvider = deviceProvider;
        this.commandExecutor = commandExecutor;
        this.logcatStreamManager = logcatStreamManager;
    }

    @GetMapping
    public DeviceDiscoveryResult getConnectedDevices() {
        Long tenantId = getTenantId();
        return deviceProvider.discoverDevices(tenantId);
    }

    @PostMapping("/{serial}/reboot")
    public DeviceMessageResult reboot(@PathVariable String serial) {
        validateSerial(serial);
        Long tenantId = getTenantId();
        return deviceProvider.reboot(tenantId, serial);
    }

    @PostMapping("/{serial}/uninstall")
    public UninstallAppResult uninstall(@PathVariable String serial, @RequestBody Map<String, String> body) {
        validateSerial(serial);
        Long tenantId = getTenantId();
        String packageName = body.getOrDefault("packageName", "");
        validatePackageName(packageName);
        return deviceProvider.uninstall(tenantId, serial, packageName);
    }

    @PostMapping("/{serial}/wifi-debug")
    public WifiDebugResult toggleWifiDebug(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        validateSerial(serial);
        Long tenantId = getTenantId();
        String ipAddress = (String) body.getOrDefault("ipAddress", "");
        boolean wifiDebugSession = (boolean) body.getOrDefault("wifiDebugSession", false);
        boolean hasWifiIp = (boolean) body.getOrDefault("hasWifiIp", false);
        return deviceProvider.toggleWifiDebug(tenantId, serial, ipAddress, wifiDebugSession, hasWifiIp);
    }

    @PostMapping("/{serial}/firebase-debug")
    public DeviceMessageResult enableFirebaseDebug(@PathVariable String serial, @RequestBody Map<String, String> body) {
        validateSerial(serial);
        Long tenantId = getTenantId();
        String packageName = body.getOrDefault("packageName", "");
        validatePackageName(packageName);
        return deviceProvider.enableFirebaseDebug(tenantId, serial, packageName);
    }

    @PostMapping("/{serial}/pull-logs")
    public DeviceMessageResult pullLogs(@PathVariable String serial) {
        validateSerial(serial);
        Long tenantId = getTenantId();
        return deviceProvider.pullLogs(tenantId, serial);
    }

    @PostMapping("/{serial}/screenshot")
    public ScreenshotCaptureResponse takeScreenshot(@PathVariable String serial, @RequestBody(required = false) Map<String, String> body) {
        validateSerial(serial);
        Long tenantId = getTenantId();
        return deviceProvider.screenshot(tenantId, serial);
    }

    @GetMapping("/{serial}/location")
    public Map<String, Object> getDeviceLocation(@PathVariable String serial) {
        validateSerial(serial);
        String output = commandExecutor.runCommand(
            "adb -s " + serial + " shell dumpsys location"
        );
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
        if (!found) {
            return Map.of("lat", 0.0, "lng", 0.0, "found", false);
        }
        return Map.of("lat", lat, "lng", lng, "found", true);
    }

    @PostMapping("/{serial}/mock-location")
    public Map<String, Object> setMockLocation(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        validateSerial(serial);
        double lat = ((Number) body.get("lat")).doubleValue();
        double lng = ((Number) body.get("lng")).doubleValue();
        boolean start = (boolean) body.getOrDefault("start", true);

        // Stop any existing mock for this device
        Process existing = activeMocks.remove(serial);
        if (existing != null) existing.destroyForcibly();

        if (!start) {
            // Remove test provider to restore real GPS
            commandExecutor.runCommand("adb -s " + serial + " shell cmd location providers remove-test-provider gps");
            return Map.of("success", true, "message", "Mock location stopped", "mocking", false);
        }

        // Setup test provider
        commandExecutor.runCommand("adb -s " + serial + " shell appops set com.android.shell android:mock_location allow");
        commandExecutor.runCommand("adb -s " + serial + " shell cmd location providers add-test-provider gps");
        commandExecutor.runCommand("adb -s " + serial + " shell cmd location providers set-test-provider-enabled gps true");

        // Start persistent loop on device injecting every 300ms
        try {
            String loopCmd = String.format(
                "while true; do cmd location providers set-test-provider-location gps --location %f,%f --accuracy 1.0; sleep 0.3; done",
                lat, lng
            );
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "shell", loopCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeMocks.put(serial, process);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to start mock: " + e.getMessage(), "mocking", false);
        }

        return Map.of(
            "success", true,
            "message", String.format("Mocking location: %.6f, %.6f", lat, lng),
            "mocking", true
        );
    }

    @GetMapping("/{serial}/logcat-data")
    public LogcatData getLogcatData(@PathVariable String serial) {
        validateSerial(serial);
        LogcatData data = logcatStreamManager.getCurrentData(serial);
        return data != null ? data : new LogcatData(serial);
    }

    @PostMapping("/{serial}/track-event")
    public Map<String, Object> startTracking(@PathVariable String serial, @RequestBody Map<String, String> body) {
        validateSerial(serial);
        String keyword = body.getOrDefault("keyword", "").trim();
        if (keyword.isEmpty()) {
            return Map.of("success", false, "message", "Keyword is required");
        }
        logcatStreamManager.startTracking(serial, keyword);
        return Map.of("success", true, "message", "Tracking started for: " + keyword);
    }

    @PostMapping("/{serial}/stop-tracking")
    public Map<String, Object> stopTracking(@PathVariable String serial) {
        validateSerial(serial);
        logcatStreamManager.stopTracking(serial);
        return Map.of("success", true, "message", "Tracking stopped");
    }

    /**
     * Extracts the tenant ID from the current request's TenantContext.
     * In standalone mode (no tenant context set), returns null which is acceptable
     * since LocalDeviceProvider ignores the tenantId parameter.
     * In SaaS mode, the TenantContextInterceptor sets this from the JWT's tenantId claim.
     */
    private Long getTenantId() {
        return TenantContext.getTenantId();
    }
}
