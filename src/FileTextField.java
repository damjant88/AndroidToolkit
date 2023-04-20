import java.awt.Dimension;
import java.io.Serial;
import javax.swing.JTextField;

public class FileTextField extends JTextField {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	public FileTextField() {
		this.setPreferredSize(new Dimension(250, 40));
		this.setBounds(102, 270, 505, 25);
		this.setEditable(false);
		this.setVisible(true);		
	}
}
