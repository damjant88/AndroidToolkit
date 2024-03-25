package Buttons;

import javax.swing.*;
import java.awt.*;

public class OpenLogButtons extends JButton{
	/**
	 *
	 */

	public OpenLogButtons() {
		this.setBounds(115, 250, 95, 30);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setEnabled(false);
		this.setVisible(false);
		this.setText("Open Log");
	}
}
