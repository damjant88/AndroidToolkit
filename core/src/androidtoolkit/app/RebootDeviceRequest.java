package androidtoolkit.app;

public class RebootDeviceRequest {

    private final String serial;
    private final String deviceName;

    public RebootDeviceRequest(String serial, String deviceName) {
        this.serial = serial;
        this.deviceName = deviceName;
    }

    public String getSerial() {
        return serial;
    }

    public String getDeviceName() {
        return deviceName;
    }
}
