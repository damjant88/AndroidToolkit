import Buttons.InstallButton;

import java.awt.Dimension;
import javax.swing.JTextField;

public class FileTextField extends JTextField {

	/**
	 * 
	 */

	public FileTextField() {
		this.setPreferredSize(new Dimension(250, 40));
		this.setBounds(102, 310, 525, 25);
		this.setEditable(false);
		this.setVisible(true);
	}
}
