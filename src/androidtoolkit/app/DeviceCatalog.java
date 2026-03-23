package androidtoolkit.app;

import androidtoolkit.service.DeviceInfoService;
import androidtoolkit.service.DeviceGateway;

import java.util.ArrayList;
import java.util.List;

public class DeviceCatalog {

    private final DeviceGateway deviceGateway;
    private final DeviceInfoService deviceInfoService;

    public DeviceCatalog(DeviceGateway deviceGateway, DeviceInfoService deviceInfoService) {
        this.deviceGateway = deviceGateway;
        this.deviceInfoService = deviceInfoService;
    }

    public ArrayList<String> connectedSerials() {
        return deviceGateway.getConnectedDevices();
    }

    public List<ConnectedDevice> loadConnectedDevices() {
        ArrayList<String> serials = connectedSerials();
        List<ConnectedDevice> devices = new ArrayList<>();
        for (int i = 0; i < serials.size(); i++) {
            String serial = serials.get(i);
            devices.add(new ConnectedDevice(
                    i,
                    serial,
                    "Device" + (i + 1),
                    deviceInfoService.load(serial)
            ));
        }
        return devices;
    }
}
