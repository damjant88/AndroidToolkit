package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceLogLocationAction implements ActionListener {

    private final Device device;

    public DeviceLogLocationAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.openLogLocation();
    }
}
