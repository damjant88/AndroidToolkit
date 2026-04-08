package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DevicePermissionsAction implements ActionListener {

    private final Device device;

    public DevicePermissionsAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.openPermissionsDialog();
    }
}
