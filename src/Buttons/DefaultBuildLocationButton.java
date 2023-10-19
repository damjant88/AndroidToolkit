package Buttons;

import javax.swing.*;
import java.awt.*;

public class DefaultBuildLocationButton extends JButton{

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