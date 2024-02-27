package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class FileButton extends JButton{
	/**
	 * 
	 */

	public FileButton(String text) {
		this.setBounds(0, 270, 100, 25);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setText(text);
	}
}