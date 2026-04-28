package androidtoolkit.ui.components;

import java.awt.*;
import javax.swing.JRadioButton;

public class DeviceSelectionRadioButton extends JRadioButton{
	/**
	 * 
	 */
	
	// If only radio button text is used...
	public DeviceSelectionRadioButton(String text) {
		this.setBounds(10,17,70,15);
		this.setFont(new Font("Calibri", Font.BOLD, 12));
		this.setVisible(false);
		this.setText(text);
	}
	// If position and dimensions are used additionally...
	// x, y - position of the button in the window
	// a, b - dimensions of the button
	public DeviceSelectionRadioButton(String text, int x, int y, int b, int a) {
		this.setBounds(x, y, b, a);
		this.setFont(new Font("Calibri", Font.BOLD, 12));
		this.setVisible(false);
		this.setText(text);
	}
}


