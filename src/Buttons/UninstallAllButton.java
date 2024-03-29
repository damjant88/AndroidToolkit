package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class UninstallAllButton extends JButton{
	/**
	 * 
	 */

	public UninstallAllButton() {
		this.setBounds(0, 255, 100, 50);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 16));
		this.setEnabled(true);
		this.setText("Uninstall All");
	}
}

