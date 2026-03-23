package androidtoolkit.app;

import androidtoolkit.service.DeviceGateway;

import java.util.ArrayList;

public class DeviceCatalog {

    private final DeviceGateway deviceGateway;

    public DeviceCatalog(DeviceGateway deviceGateway) {
        this.deviceGateway = deviceGateway;
    }

    public ArrayList<String> connectedSerials() {
        return deviceGateway.getConnectedDevices();
    }
}
