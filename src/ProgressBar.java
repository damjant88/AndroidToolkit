import java.awt.Color;
import java.awt.Font;
import javax.swing.JProgressBar;

public class ProgressBar extends JProgressBar{
	/**
	 * 
	 */

	public ProgressBar() {
		this.setBounds(102, 330, 525, 25);
		this.setStringPainted(true);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setBackground(new Color(238, 238, 238));
		this.setString("");
	}
}
