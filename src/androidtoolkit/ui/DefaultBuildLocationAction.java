package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DefaultBuildLocationAction implements ActionListener {

    private final MyFrame frame;

    public DefaultBuildLocationAction(MyFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame.chooseDefaultBuildLocationSetting();
    }
}
