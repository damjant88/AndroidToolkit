import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class DeviceTextPanes extends JTextPane {

	SimpleAttributeSet center = new SimpleAttributeSet();
	
	// Construct a device text pane using position and dimensions only
	// x, y - position of the pane in the window
	// a, b - dimensions of the pane
	public DeviceTextPanes(int x, int y, int b, int a) {
		this.setBounds(x, y, b, a);
		this.setEditable(false);
		this.setVisible(false);
		this.setBackground(new Color(238, 238, 238));
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setFont(new Font("Calibri", Font.BOLD, 16));
		StyledDocument doc4 = this.getStyledDocument();
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
		doc4.setParagraphAttributes(0, doc4.getLength(), center, false);		
	}
}
