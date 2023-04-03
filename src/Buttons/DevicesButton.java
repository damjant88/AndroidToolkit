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
	private static final long serialVersionUID = 1L;

	public DevicesButton(ImageIcon buttonIcon) {
		this.setBounds(0, 0, 198, 50);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setText("Display Connected Devices");
		this.setIcon(buttonIcon);
	}
}
