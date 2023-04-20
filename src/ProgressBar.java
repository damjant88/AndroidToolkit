import java.awt.Color;
import java.awt.Font;
import java.io.Serial;
import javax.swing.JProgressBar;

public class ProgressBar extends JProgressBar{
	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	public ProgressBar() {
		this.setBounds(102, 300, 505, 25);
		this.setStringPainted(true);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setBackground(new Color(238, 238, 238));
		this.setString("");
	}
}
