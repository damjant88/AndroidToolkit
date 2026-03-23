package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceRebootAction implements ActionListener {

    private final Device device;

    public DeviceRebootAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.rebootDevice();
    }
}
