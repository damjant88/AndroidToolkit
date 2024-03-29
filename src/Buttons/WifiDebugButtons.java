package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class WifiDebugButtons extends JButton{
	/**
	 * 
	 */
	
	public WifiDebugButtons() {
		this.setBounds(110, 160, 100, 30);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setVisible(false);
		this.setText("WiFi Debug");
	}
}
