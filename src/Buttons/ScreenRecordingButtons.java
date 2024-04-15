package Buttons;

import Buttons.DeviceButtons;

import javax.swing.*;
import java.awt.*;

public class ScreenRecordingButtons extends DeviceButtons {
	/**
	 *
	 */

	public ScreenRecordingButtons() {
		this.setBounds(105, 280, 100, 30);
//		this.setFocusable(false);
//		this.setBorder(BorderFactory.createEmptyBorder());
//		this.setBackground(Color.lightGray);
//		this.setMargin(new Insets(0, 0, 0, 0));
//		this.setFont(new Font("Calibri", Font.BOLD, 15));
//		this.setVisible(false);
		this.setText("Start Record");
	}
}
