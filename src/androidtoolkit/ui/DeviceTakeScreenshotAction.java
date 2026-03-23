package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeviceTakeScreenshotAction implements ActionListener {

    private final Device device;

    public DeviceTakeScreenshotAction(Device device) {
        this.device = device;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        device.takeScreenshot();
    }
}
