package androidtoolkit.backend.controller;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.DeviceActionManager;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.LogExportManager;
import androidtoolkit.app.LogExportResponse;
import androidtoolkit.app.ScreenshotCaptureResponse;
import androidtoolkit.app.ScreenshotManager;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static androidtoolkit.backend.validation.InputValidator.validatePackageName;
import static androidtoolkit.backend.validation.InputValidator.validateSerial;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceCatalog deviceCatalog;
    private final DeviceActionManager deviceActionManager;
    private final LogExportManager logExportManager;
    private final ScreenshotManager screenshotManager;
    private final AppServices appServices;

    public DeviceController(
            DeviceCatalog deviceCatalog,
            DeviceActionManager deviceActionManager,
            LogExportManager logExportManager,
            ScreenshotManager screenshotManager,
            AppServices appServices
    ) {
        this.deviceCatalog = deviceCatalog;
        this.deviceActionManager = deviceActionManager;
        this.logExportManager = logExportManager;
        this.screenshotManager = screenshotManager;
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
}
