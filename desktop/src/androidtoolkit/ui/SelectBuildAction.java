package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SelectBuildAction implements ActionListener {

    private final MyFrame frame;

    public SelectBuildAction(MyFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame.selectBuildFromDropdown();
    }
}
