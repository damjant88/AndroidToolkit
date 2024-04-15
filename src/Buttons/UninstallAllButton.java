package Buttons;
import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class UninstallAllButton extends JButton{
	/**
	 * 
	 */

	public UninstallAllButton() {
		this.setBounds(0, 261, 100, 50);
//		this.setFocusable(false);
//		this.setBorder(BorderFactory.createEtchedBorder());
//		this.setBackground(Color.lightGray);
		this.setMargin(new Insets(0, 0, 0, 0));
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setEnabled(true);
		this.setText("Uninstall All");
	}
}

