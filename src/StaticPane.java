import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class StaticPane extends JTextPane {

	/**
	 * 
	 */
	SimpleAttributeSet center = new SimpleAttributeSet();
	
	public StaticPane() {

		this.setBounds(0, 50, 200, 110);
		this.setEditable(false);
		this.setVisible(true);
		this.setBackground(new Color(238, 238, 238));
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setFont(new Font("Calibri", Font.BOLD, 16));
		StyledDocument doc = this.getStyledDocument();
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_RIGHT);
		doc.setParagraphAttributes(0, doc.getLength(), center, false);
		this.setText("Serial number: " + "\n" + "Manufacturer: " + "\n" + "Model: " + "\n" + "OS Version: " + "\n"
				+ "WiFi IP Address: ");
		
	}
}
