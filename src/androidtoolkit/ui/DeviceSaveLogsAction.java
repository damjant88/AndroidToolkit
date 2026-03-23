package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceSaveLogsAction implements ActionListener {

    private final Device device;

    public DeviceSaveLogsAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.saveLogs();
    }
}
