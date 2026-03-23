package androidtoolkit.ui;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.ConnectedDevice;

import java.util.ArrayList;
import java.util.List;

public class DevicePanelFactory {

    private final AppServices appServices;

    public DevicePanelFactory(AppServices appServices) {
        this.appServices = appServices;
    }

    public List<Device> createPanels(MyFrame parent, List<ConnectedDevice> connectedDevices, Runnable refreshDevicesMethod) {
        List<Device> devicePanels = new ArrayList<>();
        int totalDeviceCount = connectedDevices.size();
        for (ConnectedDevice connectedDevice : connectedDevices) {
            Device devicePanel = new Device(parent, connectedDevice, totalDeviceCount, refreshDevicesMethod, appServices);
            devicePanel.setVisible(true);
            devicePanels.add(devicePanel);
        }
        return devicePanels;
    }
}
