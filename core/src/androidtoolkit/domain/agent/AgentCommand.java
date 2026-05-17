package androidtoolkit.domain.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Commands sent from the Server to the Agent via WebSocket.
 * Each command instructs the Agent to perform a specific ADB operation
 * on a locally connected Android device.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AgentCommand.StartLogcat.class, name = "StartLogcat"),
    @JsonSubTypes.Type(value = AgentCommand.StopLogcat.class, name = "StopLogcat"),
    @JsonSubTypes.Type(value = AgentCommand.PullLogs.class, name = "PullLogs"),
    @JsonSubTypes.Type(value = AgentCommand.InstallApk.class, name = "InstallApk"),
    @JsonSubTypes.Type(value = AgentCommand.UninstallApp.class, name = "UninstallApp"),
    @JsonSubTypes.Type(value = AgentCommand.CaptureScreenshot.class, name = "CaptureScreenshot"),
    @JsonSubTypes.Type(value = AgentCommand.Reboot.class, name = "Reboot"),
    @JsonSubTypes.Type(value = AgentCommand.ToggleWifiDebug.class, name = "ToggleWifiDebug"),
    @JsonSubTypes.Type(value = AgentCommand.EnableFirebaseDebug.class, name = "EnableFirebaseDebug"),
    @JsonSubTypes.Type(value = AgentCommand.GetLocation.class, name = "GetLocation"),
    @JsonSubTypes.Type(value = AgentCommand.SetMockLocation.class, name = "SetMockLocation"),
    @JsonSubTypes.Type(value = AgentCommand.ManagePermission.class, name = "ManagePermission"),
    @JsonSubTypes.Type(value = AgentCommand.ManageAccessibility.class, name = "ManageAccessibility"),
    @JsonSubTypes.Type(value = AgentCommand.StartScreenMirror.class, name = "StartScreenMirror"),
    @JsonSubTypes.Type(value = AgentCommand.GetPackageDump.class, name = "GetPackageDump")
})
public sealed interface AgentCommand {

    /**
     * Start streaming logcat output for the specified device.
     *
     * @param serial    the device serial number
     * @param requestId a unique identifier to correlate the stream request
     */
    record StartLogcat(String serial, String requestId) implements AgentCommand {}

    /**
     * Stop an active logcat stream for the specified device.
     *
     * @param serial the device serial number
     */
    record StopLogcat(String serial) implements AgentCommand {}

    /**
     * Pull log files from the specified device to a local path.
     *
     * @param serial     the device serial number
     * @param targetPath the local path where logs should be stored
     */
    record PullLogs(String serial, String targetPath) implements AgentCommand {}

    /**
     * Install an APK on the specified device.
     *
     * @param serial the device serial number
     * @param apkUrl the URL from which to download the APK
     */
    record InstallApk(String serial, String apkUrl) implements AgentCommand {}

    /**
     * Uninstall an application from the specified device.
     *
     * @param serial      the device serial number
     * @param packageName the package name of the app to uninstall
     */
    record UninstallApp(String serial, String packageName) implements AgentCommand {}

    /**
     * Capture a screenshot from the specified device.
     *
     * @param serial the device serial number
     */
    record CaptureScreenshot(String serial) implements AgentCommand {}

    /**
     * Reboot the specified device.
     *
     * @param serial the device serial number
     */
    record Reboot(String serial) implements AgentCommand {}

    /**
     * Toggle Wi-Fi debugging on a device.
     *
     * @param serial           the device serial number
     * @param ipAddress        the device IP address
     * @param wifiDebugSession whether a Wi-Fi debug session is currently active
     * @param hasWifiIp        whether the device has a Wi-Fi IP
     */
    record ToggleWifiDebug(String serial, String ipAddress, boolean wifiDebugSession, boolean hasWifiIp) implements AgentCommand {}

    /**
     * Enable Firebase Analytics debug mode for an app.
     *
     * @param serial      the device serial number
     * @param packageName the package to enable debug for
     */
    record EnableFirebaseDebug(String serial, String packageName) implements AgentCommand {}

    /**
     * Get the device's last known GPS location.
     *
     * @param serial the device serial number
     */
    record GetLocation(String serial) implements AgentCommand {}

    /**
     * Set or stop mock GPS location on a device.
     *
     * @param serial the device serial number
     * @param lat    latitude
     * @param lng    longitude
     * @param start  true to start mocking, false to stop
     */
    record SetMockLocation(String serial, double lat, double lng, boolean start) implements AgentCommand {}

    /**
     * Manage a permission for an app on a device.
     *
     * @param serial      the device serial number
     * @param packageName the target package
     * @param permission  the permission string
     * @param action      "grant", "revoke", "addIdleWhitelist", "removeIdleWhitelist",
     *                    "ignoreAutoRevoke", "resetAutoRevoke"
     */
    record ManagePermission(String serial, String packageName, String permission, String action) implements AgentCommand {}

    /**
     * Enable or disable an accessibility service on a device.
     *
     * @param serial           the device serial number
     * @param packageName      the package containing the service
     * @param serviceClassName the accessibility service class name
     * @param enable           true to enable, false to disable
     */
    record ManageAccessibility(String serial, String packageName, String serviceClassName, boolean enable) implements AgentCommand {}

    /**
     * Start screen mirroring for a device.
     *
     * @param serial the device serial number
     */
    record StartScreenMirror(String serial) implements AgentCommand {}

    /**
     * Get the package dump (dumpsys) for an app on a device.
     *
     * @param serial      the device serial number
     * @param packageName the package to dump
     */
    record GetPackageDump(String serial, String packageName) implements AgentCommand {}
}
