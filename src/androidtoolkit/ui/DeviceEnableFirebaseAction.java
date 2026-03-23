package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceEnableFirebaseAction implements ActionListener {

    private final Device device;

    public DeviceEnableFirebaseAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.enableFirebaseDebugging();
    }
}
