package androidtoolkit.backend.device;

import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.ScreenshotCaptureResponse;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;

/**
 * Abstraction for device discovery and command execution.
 * Implementations are selected based on deployment.mode configuration.
 *
 * <p>In standalone mode, delegates to local adb via DeviceCatalog and DeviceActionManager.
 * In SaaS mode, reads device state from AgentConnectionManager and relays commands to agents.
 */
public interface DeviceProvider {

    /**
     * Discover connected devices for the given tenant.
     *
     * @param tenantId the tenant to scope the discovery to
     * @return discovery result containing device serials and connected device objects
     */
    DeviceDiscoveryResult discoverDevices(Long tenantId);

    /**
     * Reboot a device.
     *
     * @param tenantId the tenant that owns the device
     * @param serial   the device serial number
     * @return result message indicating success or failure
     */
    DeviceMessageResult reboot(Long tenantId, String serial);

    /**
     * Uninstall an application from a device.
     *
     * @param tenantId    the tenant that owns the device
     * @param serial      the device serial number
     * @param packageName the package name of the application to uninstall
     * @return result indicating whether the uninstall succeeded
     */
    UninstallAppResult uninstall(Long tenantId, String serial, String packageName);

    /**
     * Capture a screenshot from a device.
     *
     * @param tenantId the tenant that owns the device
     * @param serial   the device serial number
     * @return response containing the screenshot data or path
     */
    ScreenshotCaptureResponse screenshot(Long tenantId, String serial);

    /**
     * Pull logs from a device.
     *
     * @param tenantId the tenant that owns the device
     * @param serial   the device serial number
     * @return result message indicating success or failure
     */
    DeviceMessageResult pullLogs(Long tenantId, String serial);

    /**
     * Toggle Wi-Fi debugging on a device.
     *
     * @param tenantId         the tenant that owns the device
     * @param serial           the device serial number
     * @param ipAddress        the IP address for Wi-Fi debugging
     * @param wifiDebugSession whether a Wi-Fi debug session is active
     * @param hasWifiIp        whether the device has a Wi-Fi IP assigned
     * @return result indicating the Wi-Fi debug state
     */
    WifiDebugResult toggleWifiDebug(Long tenantId, String serial,
                                    String ipAddress, boolean wifiDebugSession, boolean hasWifiIp);

    /**
     * Enable Firebase debug mode for an application on a device.
     *
     * @param tenantId    the tenant that owns the device
     * @param serial      the device serial number
     * @param packageName the package name of the application
     * @return result message indicating success or failure
     */
    DeviceMessageResult enableFirebaseDebug(Long tenantId, String serial, String packageName);
}
