package androidtoolkit.app;

public class DeviceActionManager {

    private final DeviceOperations deviceOperations;

    public DeviceActionManager(DeviceOperations deviceOperations) {
        this.deviceOperations = deviceOperations;
    }

    public WifiDebugResult toggleWifiDebugging(String serial, String deviceName, String ipAddress, boolean wifiDebugSession, boolean hasWifiIp) {
        return deviceOperations.toggleWifiDebugging(
                new WifiDebugRequest(serial, deviceName, ipAddress, wifiDebugSession, hasWifiIp)
        );
    }

    public DeviceMessageResult enableFirebaseDebugging(String serial, String deviceName, String packageName) {
        return deviceOperations.enableFirebaseDebugging(
                new FirebaseDebugRequest(serial, deviceName, packageName)
        );
    }

    public DeviceMessageResult rebootDevice(String serial, String deviceName) {
        return deviceOperations.rebootDevice(new RebootDeviceRequest(serial, deviceName));
    }

    public UninstallAppResult uninstallApp(String serial, String deviceName, String packageName) {
        return deviceOperations.uninstallApp(
                new UninstallAppRequest(serial, deviceName, packageName)
        );
    }

    public DeviceMessageResult startScreenMirror(String serial, String deviceName) {
        return deviceOperations.startScreenMirror(new ScreenMirrorRequest(serial, deviceName));
    }
}
