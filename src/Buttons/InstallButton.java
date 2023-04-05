package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class InstallButton extends JButton{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InstallButton(int x, int y, int b, int a) {
		this.setBounds(x, y, b, a);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 25));
		this.setEnabled(false);
		this.setText("Install");
	}
}

