package androidtoolkit.domain.agent;

/**
 * Commands sent from the Server to the Agent via WebSocket.
 * Each command instructs the Agent to perform a specific ADB operation
 * on a locally connected Android device.
 */
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
}
