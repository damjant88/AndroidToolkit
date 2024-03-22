package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;

public class DevicesButton extends JButton{
	/**
	 * 
	 */

	public DevicesButton(ImageIcon buttonIcon) {
		this.setBounds(0, 0, 200, 50);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setText("Display Connected Devices");
		this.setIcon(buttonIcon);
	}
}
