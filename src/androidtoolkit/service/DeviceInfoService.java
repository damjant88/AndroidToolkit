package androidtoolkit.service;

import androidtoolkit.domain.DeviceInfo;

public class DeviceInfoService {

    private final Util utility;

    public DeviceInfoService(Util utility) {
        this.utility = utility;
    }

    public DeviceInfo load(String serial) {
        String manufacturer = utility.getDeviceManufacturer(serial);
        String model = utility.getDeviceModel(serial);
        String osVersion = utility.getDeviceOSVersion(serial);
        String safePathPackage = utility.getSafePathPackage(serial);
        boolean appInstalled = utility.checkIfInstalled(serial);
        String wifiIp = utility.getWlanIp(serial);
        String mobileIp = utility.getMobileIp(serial);
        String ipAddress = wifiIp.isEmpty() ? mobileIp : wifiIp;
        String pid = utility.runCommand("adb -s " + serial + " shell pidof -s com.smithmicro.safepath.family");
        System.out.println(pid);

        return new DeviceInfo(
                serial,
                manufacturer,
                model,
                osVersion,
                wifiIp,
                mobileIp,
                ipAddress,
                safePathPackage,
                appInstalled,
                pid
        );
    }
}
