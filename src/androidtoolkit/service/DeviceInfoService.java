package androidtoolkit.service;

import androidtoolkit.domain.DeviceInfo;

public class DeviceInfoService {

    private final AdbDeviceService adbDeviceService;
    private final CommandExecutor commandExecutor;

    public DeviceInfoService(AdbDeviceService adbDeviceService, CommandExecutor commandExecutor) {
        this.adbDeviceService = adbDeviceService;
        this.commandExecutor = commandExecutor;
    }

    public DeviceInfo load(String serial) {
        String manufacturer = adbDeviceService.getDeviceManufacturer(serial);
        String model = adbDeviceService.getDeviceModel(serial);
        String osVersion = adbDeviceService.getDeviceOSVersion(serial);
        String safePathPackage = adbDeviceService.getSafePathPackage(serial);
        boolean appInstalled = adbDeviceService.checkIfInstalled(serial);
        String wifiIp = adbDeviceService.getWlanIp(serial);
        String mobileIp = adbDeviceService.getMobileIp(serial);
        String ipAddress = wifiIp.isEmpty() ? mobileIp : wifiIp;
        String pid = commandExecutor.runCommand("adb -s " + serial + " shell pidof -s com.smithmicro.safepath.family");
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
