package Buttons;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;

public class DefaultBuildLocationButton extends JButton{
	/**
	 *
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	public DefaultBuildLocationButton(String text) {
		this.setBounds(0, 0, 50, 20);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setText(text);
		this.setVisible(true);
	}
}