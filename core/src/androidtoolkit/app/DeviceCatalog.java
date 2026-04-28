package androidtoolkit.app;

import androidtoolkit.service.DeviceInfoService;
import androidtoolkit.service.DeviceGateway;

import java.util.ArrayList;

public class DeviceCatalog {

    private final DeviceGateway deviceGateway;
    private final DeviceInfoService deviceInfoService;

    public DeviceCatalog(DeviceGateway deviceGateway, DeviceInfoService deviceInfoService) {
        this.deviceGateway = deviceGateway;
        this.deviceInfoService = deviceInfoService;
    }

    public DeviceDiscoveryResult discoverDevices(DeviceDiscoveryRequest request) {
        ArrayList<String> serials = deviceGateway.getConnectedDevices();
        ArrayList<ConnectedDevice> devices = new ArrayList<>();
        for (int i = 0; i < serials.size(); i++) {
            String serial = serials.get(i);
            devices.add(new ConnectedDevice(
                    i,
                    serial,
                    "Device" + (i + 1),
                    deviceInfoService.load(serial)
            ));
        }
        return new DeviceDiscoveryResult(serials, devices);
    }
}
