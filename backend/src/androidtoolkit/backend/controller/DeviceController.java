package androidtoolkit.backend.controller;

import androidtoolkit.app.DeviceActionManager;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API for device operations.
 *
 * Test with Postman:
 *   GET  http://localhost:8080/api/devices          → list connected devices
 *   POST http://localhost:8080/api/devices/{serial}/reboot
 *   POST http://localhost:8080/api/devices/{serial}/uninstall  (body: {"packageName": "..."})
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceCatalog deviceCatalog;
    private final DeviceActionManager deviceActionManager;

    public DeviceController(DeviceCatalog deviceCatalog, DeviceActionManager deviceActionManager) {
        this.deviceCatalog = deviceCatalog;
        this.deviceActionManager = deviceActionManager;
    }

    @GetMapping
    public DeviceDiscoveryResult getConnectedDevices() {
        return deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
    }

    @PostMapping("/{serial}/reboot")
    public DeviceMessageResult reboot(@PathVariable String serial) {
        return deviceActionManager.rebootDevice(serial, serial);
    }

    @PostMapping("/{serial}/uninstall")
    public UninstallAppResult uninstall(@PathVariable String serial, @RequestBody Map<String, String> body) {
        String packageName = body.getOrDefault("packageName", "");
        return deviceActionManager.uninstallApp(serial, serial, packageName);
    }

    @PostMapping("/{serial}/wifi-debug")
    public WifiDebugResult toggleWifiDebug(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        String ipAddress = (String) body.getOrDefault("ipAddress", "");
        boolean wifiDebugSession = (boolean) body.getOrDefault("wifiDebugSession", false);
        boolean hasWifiIp = (boolean) body.getOrDefault("hasWifiIp", false);
        return deviceActionManager.toggleWifiDebugging(serial, serial, ipAddress, wifiDebugSession, hasWifiIp);
    }

    @PostMapping("/{serial}/firebase-debug")
    public DeviceMessageResult enableFirebaseDebug(@PathVariable String serial, @RequestBody Map<String, String> body) {
        String packageName = body.getOrDefault("packageName", "");
        return deviceActionManager.enableFirebaseDebugging(serial, serial, packageName);
    }
}
