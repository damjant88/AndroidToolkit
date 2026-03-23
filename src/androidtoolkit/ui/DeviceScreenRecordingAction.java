package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceScreenRecordingAction implements ActionListener {

    private final Device device;

    public DeviceScreenRecordingAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.handleScreenRecording();
    }
}
