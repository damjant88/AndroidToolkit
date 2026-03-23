package androidtoolkit.app;

public class DeviceTarget {

    private final String serial;
    private final String deviceName;
    private final boolean selectedForInstall;
    private final boolean appInstalled;

    public DeviceTarget(String serial, String deviceName, boolean selectedForInstall, boolean appInstalled) {
        this.serial = serial;
        this.deviceName = deviceName;
        this.selectedForInstall = selectedForInstall;
        this.appInstalled = appInstalled;
    }

    public String getSerial() {
        return serial;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isSelectedForInstall() {
        return selectedForInstall;
    }

    public boolean isAppInstalled() {
        return appInstalled;
    }
}
