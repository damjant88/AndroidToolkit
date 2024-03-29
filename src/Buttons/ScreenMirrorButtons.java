package Buttons;

import javax.swing.*;
import java.awt.*;

public class ScreenMirrorButtons extends JButton{
	/**
	 *
	 */

	public ScreenMirrorButtons() {
		this.setBounds(0, 280, 100, 30);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setVisible(false);
		this.setText("Screen Mirror");
	}
}
