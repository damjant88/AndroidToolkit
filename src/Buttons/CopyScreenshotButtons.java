package Buttons;

import javax.swing.*;
import java.awt.*;

public class CopyScreenshotButtons extends JButton{
	/**
	 *
	 */

	public CopyScreenshotButtons() {
		this.setBounds(125, 0, 120, 30);
//		this.setFocusable(false);
//		this.setBorder(BorderFactory.createEtchedBorder());
//		this.setBackground(Color.lightGray);
		this.setMargin(new Insets(0, 0, 0, 0));
		this.setFont(new Font("Calibri", Font.BOLD, 14));
		this.setVisible(false);
		this.setText("Copy to clipboard");
	}
}
