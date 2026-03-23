package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceUninstallAppAction implements ActionListener {

    private final Device device;

    public DeviceUninstallAppAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.uninstallApp();
    }
}
