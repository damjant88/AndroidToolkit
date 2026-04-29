package androidtoolkit.backend.controller;

import androidtoolkit.app.DeviceActionManager;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.LogExportManager;
import androidtoolkit.app.LogExportResponse;
import androidtoolkit.app.PermissionDialogState;
import androidtoolkit.app.PermissionManager;
import androidtoolkit.app.PermissionDefinition;
import androidtoolkit.app.PermissionUpdateResponse;
import androidtoolkit.app.RecordingActionResponse;
import androidtoolkit.app.RecordingManager;
import androidtoolkit.app.ScreenshotCaptureResponse;
import androidtoolkit.app.ScreenshotManager;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;
import androidtoolkit.app.AppServices;
import androidtoolkit.domain.RecordingSession;
import androidtoolkit.service.ScreenRecordingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static androidtoolkit.backend.validation.InputValidator.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceCatalog deviceCatalog;
    private final DeviceActionManager deviceActionManager;
    private final LogExportManager logExportManager;
    private final ScreenshotManager screenshotManager;
    private final PermissionManager permissionManager;
    private final RecordingManager recordingManager;
    private final ScreenRecordingService screenRecordingService;
    private final AppServices appServices;
    private final Map<String, RecordingSession> recordingSessions = new ConcurrentHashMap<>();

    public DeviceController(
            DeviceCatalog deviceCatalog,
            DeviceActionManager deviceActionManager,
            LogExportManager logExportManager,
            ScreenshotManager screenshotManager,
            PermissionManager permissionManager,
            RecordingManager recordingManager,
            AppServices appServices
    ) {
        this.deviceCatalog = deviceCatalog;
        this.deviceActionManager = deviceActionManager;
        this.logExportManager = logExportManager;
        this.screenshotManager = screenshotManager;
        this.permissionManager = permissionManager;
        this.recordingManager = recordingManager;
        this.screenRecordingService = appServices.screenRecordingService();
        this.appServices = appServices;
    }

    @GetMapping
    public DeviceDiscoveryResult getConnectedDevices() {
        return deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
    }

    @PostMapping("/{serial}/reboot")
    public DeviceMessageResult reboot(@PathVariable String serial) {
        validateSerial(serial);
        return deviceActionManager.rebootDevice(serial, serial);
    }

    @PostMapping("/{serial}/uninstall")
    public UninstallAppResult uninstall(@PathVariable String serial, @RequestBody Map<String, String> body) {
        validateSerial(serial);
        String packageName = body.getOrDefault("packageName", "");
        validatePackageName(packageName);
        return deviceActionManager.uninstallApp(serial, serial, packageName);
    }

    @PostMapping("/{serial}/wifi-debug")
    public WifiDebugResult toggleWifiDebug(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        validateSerial(serial);
        String ipAddress = (String) body.getOrDefault("ipAddress", "");
        boolean wifiDebugSession = (boolean) body.getOrDefault("wifiDebugSession", false);
        boolean hasWifiIp = (boolean) body.getOrDefault("hasWifiIp", false);
        return deviceActionManager.toggleWifiDebugging(serial, serial, ipAddress, wifiDebugSession, hasWifiIp);
    }

    @PostMapping("/{serial}/firebase-debug")
    public DeviceMessageResult enableFirebaseDebug(@PathVariable String serial, @RequestBody Map<String, String> body) {
        validateSerial(serial);
        String packageName = body.getOrDefault("packageName", "");
        validatePackageName(packageName);
        return deviceActionManager.enableFirebaseDebugging(serial, serial, packageName);
    }

    @PostMapping("/{serial}/pull-logs")
    public LogExportResponse pullLogs(@PathVariable String serial) {
        validateSerial(serial);
        String targetFolder = appServices.storagePaths().logsDir().getPath();
        return logExportManager.exportDeviceLogs(serial, serial, targetFolder);
    }

    @PostMapping("/{serial}/screenshot")
    public ScreenshotCaptureResponse takeScreenshot(@PathVariable String serial, @RequestBody(required = false) Map<String, String> body) {
        validateSerial(serial);
        String deviceName = (body != null) ? body.getOrDefault("deviceName", serial) : serial;
        return screenshotManager.captureScreenshot(serial, deviceName);
    }

    @PostMapping("/{serial}/screen-mirror")
    public Map<String, Object> startScreenMirror(@PathVariable String serial) {
        validateSerial(serial);
        try {
            screenRecordingService.startScreenMirrorAsync(serial);
            return Map.of("success", true, "message", "Screen mirror started for " + serial);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/{serial}/start-recording")
    public Map<String, Object> startRecording(@PathVariable String serial) {
        validateSerial(serial);
        RecordingSession session = recordingSessions.computeIfAbsent(serial, k -> new RecordingSession());
        if (session.isActive()) {
            return Map.of("success", false, "message", "Recording already in progress on " + serial);
        }
        try {
            RecordingActionResponse result = recordingManager.startRecording(serial, serial, session);
            return Map.of("success", result.isSuccess(), "message", result.getMessage());
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/{serial}/stop-recording")
    public Map<String, Object> stopRecording(@PathVariable String serial, @RequestBody(required = false) Map<String, String> body) {
        validateSerial(serial);
        RecordingSession session = recordingSessions.get(serial);
        if (session == null || !session.isActive()) {
            return Map.of("success", false, "message", "No active recording on " + serial);
        }
        String pid = (body != null) ? body.getOrDefault("pid", "") : "";
        String recordingFileName = session.getRecordingFileName();
        try {
            RecordingActionResponse result = recordingManager.stopRecording(serial, serial, pid, session);
            return Map.of(
                    "success", result.isSuccess(),
                    "message", result.getMessage(),
                    "recordingLocation", result.getRecordingLocation() != null ? result.getRecordingLocation() : "",
                    "recordingFileName", recordingFileName != null ? recordingFileName : ""
            );
        } catch (Exception e) {
            session.setRecordingProcess(null);
            session.getRecordingInProgress().set(false);
            return Map.of("success", false, "message", "Stop recording failed: " + e.getMessage());
        }
    }

    @GetMapping("/{serial}/permissions")
    public PermissionDialogState getPermissions(@PathVariable String serial, @RequestParam String packageName) {
        validateSerial(serial);
        validatePackageName(packageName);
        return permissionManager.loadDialogState(serial, packageName);
    }

    @PostMapping("/{serial}/permissions/enable")
    public PermissionUpdateResponse enablePermissions(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        validateSerial(serial);
        String packageName = (String) body.getOrDefault("packageName", "");
        validatePackageName(packageName);
        List<String> permissionIds = (List<String>) body.getOrDefault("permissionIds", List.of());
        List<PermissionDefinition> definitions = permissionManager.loadDialogState(serial, packageName)
                .getDefinitions().stream()
                .filter(d -> permissionIds.contains(d.getId()))
                .toList();
        return permissionManager.enablePermissions(serial, packageName, definitions);
    }

    @PostMapping("/{serial}/permissions/disable")
    public PermissionUpdateResponse disablePermissions(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        validateSerial(serial);
        String packageName = (String) body.getOrDefault("packageName", "");
        validatePackageName(packageName);
        List<String> permissionIds = (List<String>) body.getOrDefault("permissionIds", List.of());
        List<PermissionDefinition> definitions = permissionManager.loadDialogState(serial, packageName)
                .getDefinitions().stream()
                .filter(d -> permissionIds.contains(d.getId()))
                .toList();
        return permissionManager.disablePermissions(serial, packageName, definitions);
    }
}
