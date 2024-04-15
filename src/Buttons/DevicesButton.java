package Buttons;
import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;

public class DevicesButton extends JButton{
	/**
	 * 
	 */

	public DevicesButton(ImageIcon buttonIcon) {
		this.setBounds(0, 0, 199, 50);
//		this.setFocusable(false);
//		this.setBorder(BorderFactory.createEtchedBorder());
//		this.setBackground(Color.LIGHT_GRAY);
		this.setMargin(new Insets(0, 0, 0, 0));
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setText("Display Connected Devices");
		this.setEnabled(true);
		this.setVisible(true);
		this.setIcon(buttonIcon);
	}
}
