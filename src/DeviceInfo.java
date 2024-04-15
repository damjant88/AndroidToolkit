public class DeviceInfo {
    Util utility;
    String manufacturer;
    String model;
    String OSVersion;
    String wifiIP;
    String ip;
    String mobileIP;
    String safePathPackage;
    Boolean appIsInstalled;
    String serialNo;
    String pid;

    public DeviceInfo(String serial) {
            serialNo = serial;
            utility = new Util();
            manufacturer = utility.getDeviceManufacturer(serial);
            model = utility.getDeviceModel(serial);
            OSVersion = utility.getDeviceOSVersion(serial);
            safePathPackage = utility.getSafePathPackage(serial);
            appIsInstalled = utility.checkIfInstalled(serial);
            wifiIP = utility.getWlanIp(serial);
            mobileIP = utility.getMobileIp(serial);
            ip = wifiIP;
            if (wifiIP.isEmpty()) {
                ip = mobileIP;
            }
            pid = utility.runCommand("adb -s " + serialNo + " shell pidof -s com.smithmicro.safepath.family");
            System.out.println(pid);
    }
}
