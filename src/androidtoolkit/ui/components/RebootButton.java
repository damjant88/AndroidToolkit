package androidtoolkit.ui.components;

import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class RebootButton extends DeviceActionButton {
	/**
	 * 
	 */

	public RebootButton() {
		this.setBounds(0, 250, 100, 30);
//		this.setFocusable(false);
//		this.setBorder(BorderFactory.createEtchedBorder());
//		this.setBackground(Color.lightGray);
		this.setMargin(new Insets(0, 0, 0, 0));
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setVisible(false);
		this.setText("Reboot");
	}
}


