package Buttons;
import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class FileButton extends JButton{
	/**
	 * 
	 */

	public FileButton(String text) {
		this.setBounds(0, 313, 100, 25);
//		this.setFocusable(false);
//		this.setBorder(BorderFactory.createEtchedBorder());
//		this.setBackground(Color.lightGray);
		this.setMargin(new Insets(0, 0, 0, 0));
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setText(text);
	}
}