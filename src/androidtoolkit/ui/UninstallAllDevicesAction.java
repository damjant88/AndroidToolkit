package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UninstallAllDevicesAction implements ActionListener {

    private final MyFrame frame;

    public UninstallAllDevicesAction(MyFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame.startUninstallInstalledDevices();
    }
}
