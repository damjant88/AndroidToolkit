import javax.swing.*;
import java.awt.*;

public class DefaultBuildLocationTextField extends JTextField {
	/**
	 *
	 */

	public DefaultBuildLocationTextField() {
		this.setPreferredSize(new Dimension(250, 40));
		this.setBounds(102, 270, 505, 25);
		this.setEditable(false);
		this.setVisible(true);		
	}
}
