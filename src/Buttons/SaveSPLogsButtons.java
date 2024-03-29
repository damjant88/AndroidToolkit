package Buttons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class SaveSPLogsButtons extends JButton{
	/**
	 * 
	 */

	public SaveSPLogsButtons() {
		this.setBounds(0, 160, 100, 30);
		this.setFocusable(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setBackground(Color.lightGray);
		this.setFont(new Font("Calibri", Font.BOLD, 15));
		this.setVisible(false);
		this.setText("Pull SP Logs");
	}
}