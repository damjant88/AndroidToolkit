package androidtoolkit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConsoleViewToggleAction implements ActionListener {

    private final MyFrame frame;

    public ConsoleViewToggleAction(MyFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame.toggleConsoleView();
    }
}
