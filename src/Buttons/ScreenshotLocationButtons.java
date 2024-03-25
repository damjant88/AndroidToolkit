package Buttons;

import javax.swing.*;
import java.awt.*;

public class ScreenshotLocationButtons extends JButton{
	/**
	 *
	 */

	public ScreenshotLocationButtons() {
		this.setBounds(0, 0, 120, 30);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setVisible(false);
		this.setText("Screenshot location");
	}
}
