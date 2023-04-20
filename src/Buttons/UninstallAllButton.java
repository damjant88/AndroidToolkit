package Buttons;
import java.awt.Color;
import java.awt.Font;
import java.io.Serial;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class UninstallAllButton extends JButton{
	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	public UninstallAllButton(int x, int y, int b, int a) {
		this.setBounds(x, y, b, a);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 16));
		this.setEnabled(false);
		this.setText("Uninstall All");
	}
}

