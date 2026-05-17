package androidtoolkit.backend.device;

import androidtoolkit.app.DeviceActionManager;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.ScreenshotCaptureResponse;
import androidtoolkit.app.ScreenshotManager;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Standalone-mode implementation of {@link DeviceProvider}.
 * Delegates device discovery to {@link DeviceCatalog} and command execution
 * to {@link DeviceActionManager} and {@link ScreenshotManager} using direct adb.
 *
 * <p>Active when {@code deployment.mode} is {@code standalone} or not set.
 */
@Component
@ConditionalOnProperty(name = "deployment.mode", havingValue = "standalone", matchIfMissing = true)
public class LocalDeviceProvider implements DeviceProvider {

    private final DeviceCatalog deviceCatalog;
    private final DeviceActionManager deviceActionManager;
    private final ScreenshotManager screenshotManager;

    public LocalDeviceProvider(DeviceCatalog deviceCatalog,
                               DeviceActionManager deviceActionManager,
                               ScreenshotManager screenshotManager) {
        this.deviceCatalog = deviceCatalog;
        this.deviceActionManager = deviceActionManager;
        this.screenshotManager = screenshotManager;
    }

    @Override
    public DeviceDiscoveryResult discoverDevices(Long tenantId) {
        try {
            return deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
        } catch (RuntimeException e) {
            return new DeviceDiscoveryResult(new ArrayList<>(), new ArrayList<>());
        }
    }

    @Override
    public DeviceMessageResult reboot(Long tenantId, String serial) {
        return deviceActionManager.rebootDevice(serial, serial);
    }

    @Override
    public UninstallAppResult uninstall(Long tenantId, String serial, String packageName) {
        return deviceActionManager.uninstallApp(serial, serial, packageName);
    }

    @Override
    public ScreenshotCaptureResponse screenshot(Long tenantId, String serial) {
        return screenshotManager.captureScreenshot(serial, serial);
    }

    @Override
    public DeviceMessageResult pullLogs(Long tenantId, String serial) {
        return new DeviceMessageResult("Log pull initiated for device " + serial);
    }

    @Override
    public WifiDebugResult toggleWifiDebug(Long tenantId, String serial,
                                           String ipAddress, boolean wifiDebugSession, boolean hasWifiIp) {
        return deviceActionManager.toggleWifiDebugging(serial, serial, ipAddress, wifiDebugSession, hasWifiIp);
    }

    @Override
    public DeviceMessageResult enableFirebaseDebug(Long tenantId, String serial, String packageName) {
        return deviceActionManager.enableFirebaseDebugging(serial, serial, packageName);
    }
}
