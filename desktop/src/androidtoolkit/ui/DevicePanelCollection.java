package androidtoolkit.ui;

import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceDiscoveryResult;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DevicePanelCollection {

    private final Container parent;
    private final DevicePanelFactory devicePanelFactory;
    private final Runnable refreshDevicesMethod;
    private ArrayList<String> serials = new ArrayList<>();
    private List<Device> panels = new ArrayList<>();
    private int deviceCount;
    private boolean hasInstallableDevice;
    private boolean hasInstalledDevice;

    public DevicePanelCollection(Container parent, DevicePanelFactory devicePanelFactory, Runnable refreshDevicesMethod) {
        this.parent = parent;
        this.devicePanelFactory = devicePanelFactory;
        this.refreshDevicesMethod = refreshDevicesMethod;
    }

    public void replace(MyFrame frame, DeviceDiscoveryResult discoveryResult) {
        clearPanels();
        List<ConnectedDevice> connectedDevices = discoveryResult.getDevices();
        serials = new ArrayList<>(connectedDevices.stream()
                .map(ConnectedDevice::getSerial)
                .collect(Collectors.toList()));
        deviceCount = discoveryResult.getDeviceCount();
        panels = devicePanelFactory.createPanels(frame, connectedDevices, refreshDevicesMethod);
        hasInstallableDevice = false;
        hasInstalledDevice = false;
        for (Device panel : panels) {
            parent.add(panel);
            hasInstalledDevice |= panel.isAppInstalled();
            hasInstallableDevice |= !panel.isAppInstalled();
        }
    }

    public ArrayList<String> currentSerials() {
        return new ArrayList<>(serials);
    }

    public List<Device> panels() {
        return panels;
    }

    public int deviceCount() {
        return deviceCount;
    }

    public boolean hasInstallableDevice() {
        return hasInstallableDevice;
    }

    public boolean hasInstalledDevice() {
        return hasInstalledDevice;
    }

    private void clearPanels() {
        for (Device panel : panels) {
            parent.remove(panel);
        }
        panels = new ArrayList<>();
        hasInstallableDevice = false;
        hasInstalledDevice = false;
    }
}
