package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RefreshDevicesAction implements ActionListener {

    private final MyFrame frame;

    public RefreshDevicesAction(MyFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame.showRefreshedDevices();
    }
}
