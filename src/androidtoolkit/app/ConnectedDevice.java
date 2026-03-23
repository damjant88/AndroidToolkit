package androidtoolkit.app;

import androidtoolkit.domain.DeviceInfo;

public class ConnectedDevice {

    private final int index;
    private final String serial;
    private final String deviceName;
    private final DeviceInfo deviceInfo;

    public ConnectedDevice(int index, String serial, String deviceName, DeviceInfo deviceInfo) {
        this.index = index;
        this.serial = serial;
        this.deviceName = deviceName;
        this.deviceInfo = deviceInfo;
    }

    public int getIndex() {
        return index;
    }

    public String getSerial() {
        return serial;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }
}
