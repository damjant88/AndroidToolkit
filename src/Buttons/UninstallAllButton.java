package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class UninstallAllButton extends JButton{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UninstallAllButton(String text) {
		this.setBounds(0, 200, 100, 50);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 16));
		this.setEnabled(false);
		this.setText(text);
	}
}

