package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceToggleWifiDebugAction implements ActionListener {

    private final Device device;

    public DeviceToggleWifiDebugAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.toggleWifiDebugging();
    }
}
