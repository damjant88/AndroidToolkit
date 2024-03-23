import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ScreenshotFrame extends JFrame {

	String deviceName;
	int numberOfDevices;
	Icons icon;

	public ScreenshotFrame(String deviceName, int numberOfDevices) {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JLabel imageLabel = new JLabel();
		imageLabel.setHorizontalAlignment(JLabel.CENTER);
		imageLabel.setVerticalAlignment(JLabel.CENTER);
		icon = new Icons();
		this.setTitle(deviceName);
		this.setIconImage(icon.frameIcon.getImage());
		this.setResizable(false);
		this.deviceName = deviceName;
		this.numberOfDevices = numberOfDevices;
		int x = numberOfDevices * 210 + 220;
		try {
				BufferedImage image = ImageIO.read(new File("C:/AdbToolkit/Screenshots/" + deviceName + "/screenshot.png"));
				ImageIcon icon = new ImageIcon(image);
				Image scaledImage = icon.getImage().getScaledInstance(240, 520, Image.SCALE_SMOOTH);
				ImageIcon scaledIcon = new ImageIcon(scaledImage); // Create a new ImageIcon from the scaled Image
				imageLabel.setIcon(scaledIcon);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error loading image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}

		getContentPane().add(imageLabel, BorderLayout.CENTER);

		// Set the size of the JFrame to match the size of the image
		setSize(imageLabel.getIcon().getIconWidth(), imageLabel.getIcon().getIconHeight());

		if(numberOfDevices <= 2) {
			setLocation(640, 0);
		} else {
			setLocation(x, 0);
		}
		setVisible(true);
	}

}


