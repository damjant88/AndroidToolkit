package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceScreenMirrorAction implements ActionListener {

    private final Device device;

    public DeviceScreenMirrorAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.startScreenMirror();
    }
}
