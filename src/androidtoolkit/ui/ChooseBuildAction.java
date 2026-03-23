package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChooseBuildAction implements ActionListener {

    private final MyFrame frame;

    public ChooseBuildAction(MyFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame.chooseBuild();
    }
}
