package androidtoolkit.app;

public class ScreenMirrorRequest {

    private final String serial;
    private final String deviceName;

    public ScreenMirrorRequest(String serial, String deviceName) {
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
