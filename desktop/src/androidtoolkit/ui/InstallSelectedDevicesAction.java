package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InstallSelectedDevicesAction implements ActionListener {

    private final MyFrame frame;

    public InstallSelectedDevicesAction(MyFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame.startInstallSelectedDevices();
    }
}
