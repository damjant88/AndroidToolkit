import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class FileTextFieldBox extends JComboBox<String> {

	public FileTextFieldBox(ArrayList<String> values) {
		super(values.toArray(new String[0])); // Pass ArrayList values to the JComboBox constructor
		this.setPreferredSize(new Dimension(250, 40));
		this.setBounds(102, 313, 527, 25);
		this.setBackground(Color.WHITE);
		this.setEditable(false);
		this.setVisible(true);
		this.setMaximumRowCount(5);
	}
}

