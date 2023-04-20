import javax.swing.*;
import java.awt.*;
import java.io.Serial;

public class DefaultBuildLocationTextField extends JTextField {
	/**
	 *
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	public DefaultBuildLocationTextField() {
		this.setPreferredSize(new Dimension(250, 40));
		this.setBounds(102, 270, 505, 25);
		this.setEditable(false);
		this.setVisible(true);		
	}
}
