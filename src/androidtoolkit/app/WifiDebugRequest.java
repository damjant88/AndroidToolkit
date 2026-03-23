package androidtoolkit.app;

public class WifiDebugRequest {

    private final String serial;
    private final String deviceName;
    private final String ipAddress;
    private final boolean wifiDebugSession;
    private final boolean hasWifiIp;

    public WifiDebugRequest(String serial, String deviceName, String ipAddress, boolean wifiDebugSession, boolean hasWifiIp) {
        this.serial = serial;
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.wifiDebugSession = wifiDebugSession;
        this.hasWifiIp = hasWifiIp;
    }

    public String getSerial() {
        return serial;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isWifiDebugSession() {
        return wifiDebugSession;
    }

    public boolean hasWifiIp() {
        return hasWifiIp;
    }
}
