package androidtoolkit.service;

import androidtoolkit.domain.DeviceInfo;

public class DeviceInfoService {

    private final DeviceGateway deviceGateway;
    private final CommandExecutor commandExecutor;

    public DeviceInfoService(DeviceGateway deviceGateway, CommandExecutor commandExecutor) {
        this.deviceGateway = deviceGateway;
        this.commandExecutor = commandExecutor;
    }

    public DeviceInfo load(String serial) {
        String manufacturer = deviceGateway.getDeviceManufacturer(serial);
        String model = deviceGateway.getDeviceModel(serial);
        String osVersion = deviceGateway.getDeviceOSVersion(serial);
        String safePathPackage = deviceGateway.getSafePathPackage(serial);
        boolean appInstalled = deviceGateway.checkIfInstalled(serial);
        String wifiIp = deviceGateway.getWlanIp(serial);
        String mobileIp = deviceGateway.getMobileIp(serial);
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
