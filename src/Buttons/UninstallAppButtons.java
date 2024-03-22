package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class UninstallAppButtons extends JButton{
	/**
	 * 
	 */

	public UninstallAppButtons() {
		this.setBounds(115, 220, 95, 30);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setEnabled(false);
		this.setVisible(false);
		this.setText("Uninstall");
	}
}
