package Buttons;

import java.awt.Font;
import javax.swing.JRadioButton;

public class RadioButtons extends JRadioButton{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// If only radio button text is used...
	public RadioButtons(String text) {
		this.setBounds(200, 20, 75, 15);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setVisible(false);
		this.setText(text);
	}
	// If position and dimensions are used additionally...
	// x, y - position of the button in the window
	// a, b - dimensions of the button
	public RadioButtons(String text, int x, int y, int b, int a) {
		this.setBounds(x, y, b, a);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setVisible(false);
		this.setText(text);
	}
}
