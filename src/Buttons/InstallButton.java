package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class InstallButton extends JButton {
	/**
	 * 
	 */

	public InstallButton() {
		this.setBounds(0, 342, 100, 50);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 25));
		this.setEnabled(false);
		this.setText("Install");
	}
}

