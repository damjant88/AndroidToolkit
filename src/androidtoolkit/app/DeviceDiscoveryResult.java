package androidtoolkit.app;

import java.util.ArrayList;
import java.util.List;

public class DeviceDiscoveryResult {

    private final ArrayList<String> serials;
    private final ArrayList<ConnectedDevice> devices;

    public DeviceDiscoveryResult(List<String> serials, List<ConnectedDevice> devices) {
        this.serials = new ArrayList<>(serials);
        this.devices = new ArrayList<>(devices);
    }

    public ArrayList<String> getSerials() {
        return new ArrayList<>(serials);
    }

    public ArrayList<ConnectedDevice> getDevices() {
        return new ArrayList<>(devices);
    }

    public int getDeviceCount() {
        return devices.size();
    }
}
