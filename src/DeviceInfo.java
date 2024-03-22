public class DeviceInfo {
    Util utility;
    String manufacturer;
    String model;
    String OSVersion;
    String wifi;
    String safePathPackage;
    Boolean appIsInstalled;
    String serialNo;

    public DeviceInfo(String serial) {
            serialNo = serial;
            utility = new Util();
            manufacturer = utility.getDeviceManufacturer(serial);
            model = utility.getDeviceModel(serial);
            OSVersion = utility.getDeviceOSVersion(serial);
            safePathPackage = utility.getSafePathPackage(serial);
            appIsInstalled = utility.checkIfInstalled(serial);
            wifi = utility.getWlanIp(serial);
    }
}
