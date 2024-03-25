package Buttons;

import javax.swing.*;
import java.awt.*;

public class CopyScreenshotButtons extends JButton{
	/**
	 *
	 */

	public CopyScreenshotButtons() {
		this.setBounds(120, 0, 120, 30);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setVisible(false);
		this.setText("Copy to clipboard");
	}
}
