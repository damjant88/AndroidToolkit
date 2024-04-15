package Buttons;

import javax.swing.*;
import java.awt.*;

public class ScreenshotLocationButtons extends JButton{
	/**
	 *
	 */

	public ScreenshotLocationButtons() {
		this.setBounds(0, 0, 125, 30);
//		this.setFocusable(false);
//		this.setBorder(BorderFactory.createEtchedBorder());
//		this.setBackground(Color.lightGray);
//		this.setMargin(new Insets(0, 0, 0, 0));
		this.setFont(new Font("Calibri", Font.BOLD, 14));
//		this.setVisible(false);
		this.setMargin(new Insets(0, 0, 0, 0));
		this.setText("Screenshot location");
	}
}
