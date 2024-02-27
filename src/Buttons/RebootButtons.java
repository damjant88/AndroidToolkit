package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class RebootButtons extends JButton{
	/**
	 * 
	 */

	public RebootButtons(int x, int y, int b, int a) {
		this.setBounds(x, y, b, a);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setVisible(false);
		this.setText("Reboot");
	}
}
