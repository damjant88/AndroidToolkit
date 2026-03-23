package androidtoolkit.app;

public class FirebaseDebugRequest {

    private final String serial;
    private final String deviceName;
    private final String packageName;

    public FirebaseDebugRequest(String serial, String deviceName, String packageName) {
        this.serial = serial;
        this.deviceName = deviceName;
        this.packageName = packageName;
    }

    public String getSerial() {
        return serial;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getPackageName() {
        return packageName;
    }
}
