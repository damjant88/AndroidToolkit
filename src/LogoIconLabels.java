import java.awt.Font;
import java.io.Serial;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class LogoIconLabels extends JLabel{
	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	// If only image icon is used to construct...
	public LogoIconLabels(ImageIcon icon) {
		this.setBounds(905, 0, 122, 50);
		this.setIcon(icon);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setVisible(false);
	}
	
	// If position and dimensions are used additionally...
	// x, y - position of the icon in the window
	// a, b - dimensions of the icon
	public LogoIconLabels(ImageIcon icon, int x, int y, int b, int a) {
		this.setBounds(x, y, b, a);
		this.setIcon(icon);
		this.setFont(new Font("Calibri", Font.BOLD, 13));
		this.setVisible(false);
	}
}