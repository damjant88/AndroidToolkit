package Buttons;
import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class DeviceButtons extends JButton {
    /**
     *
     */

    public DeviceButtons() {
//        this.setBounds(0, 220, 100, 30);
//		this.setFocusable(false);
//		this.setBorder(BorderFactory.createEtchedBorder());
//		this.setBackground(Color.lightGray);
        this.setMargin(new Insets(0, 0, 0, 0));
        this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setEnabled(false);
        this.setVisible(false);
//        this.setText("Firebase");
    }
}
