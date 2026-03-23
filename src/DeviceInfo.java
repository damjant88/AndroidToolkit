public class DeviceInfo {

    private final String serialNumber;
    private final String manufacturer;
    private final String model;
    private final String osVersion;
    private final String wifiIp;
    private final String mobileIp;
    private final String ipAddress;
    private final String safePathPackage;
    private final boolean appInstalled;
    private final String pid;

    public DeviceInfo(
            String serialNumber,
            String manufacturer,
            String model,
            String osVersion,
            String wifiIp,
            String mobileIp,
            String ipAddress,
            String safePathPackage,
            boolean appInstalled,
            String pid
    ) {
        this.serialNumber = serialNumber;
        this.manufacturer = manufacturer;
        this.model = model;
        this.osVersion = osVersion;
        this.wifiIp = wifiIp;
        this.mobileIp = mobileIp;
        this.ipAddress = ipAddress;
        this.safePathPackage = safePathPackage;
        this.appInstalled = appInstalled;
        this.pid = pid;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getWifiIp() {
        return wifiIp;
    }

    public String getMobileIp() {
        return mobileIp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getSafePathPackage() {
        return safePathPackage;
    }

    public boolean isAppInstalled() {
        return appInstalled;
    }

    public String getPid() {
        return pid;
    }

    public boolean isWifiDebugSession() {
        return serialNumber.endsWith(":5555");
    }

    public boolean hasWifiIp() {
        return !wifiIp.isEmpty();
    }

    public String toDisplayText() {
        return serialNumber + "\n" + manufacturer + "\n" + model + "\n" + "Android " + osVersion + "\n" + ipAddress;
    }
}
