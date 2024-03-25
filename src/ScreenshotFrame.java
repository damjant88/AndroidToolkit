import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import Buttons.*;

public class ScreenshotFrame extends JFrame {

	String deviceName;
	int numberOfDevices;
	Icons icon;
	ScreenshotLocationButtons screenshotLocationButton;
	CopyScreenshotButtons copyScreenshotButton;

	public ScreenshotFrame(String deviceName, int numberOfDevices) {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JLabel imageLabel = new JLabel();
		imageLabel.setBounds(0, 30, 240, 520);
		icon = new Icons();
		screenshotLocationButton = new ScreenshotLocationButtons();
		screenshotLocationButton.addActionListener(new ScreenshotLocationButtonListener());
		screenshotLocationButton.setVisible(true);
		screenshotLocationButton.setOpaque(true);
		this.add(screenshotLocationButton);

		copyScreenshotButton = new CopyScreenshotButtons();
		copyScreenshotButton.addActionListener(new CopyScreenshotButtonListener());
		copyScreenshotButton.setVisible(true);
		copyScreenshotButton.setOpaque(true);
		this.add(copyScreenshotButton);

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
		setSize(imageLabel.getIcon().getIconWidth() + 16, imageLabel.getIcon().getIconHeight() + 30);

		if(numberOfDevices <= 2) {
			setLocation(640, 0);
		} else {
			setLocation(x, 0);
		}
		setVisible(true);
	}

	class ScreenshotLocationButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			openExplorerToFolder("C:/AdbToolkit/Screenshots/" + deviceName);
		}
	}

	class CopyScreenshotButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {
			File imageFile = new File("C:/AdbToolkit/Screenshots/" + deviceName + "/screenshot.png");
			System.out.println("C:/AdbToolkit/Screenshots/" + deviceName + "/screenshot.png");
			if (!imageFile.exists()) {
				System.err.println("Image file does not exist.");
				return;
			}

			try {
				// Read the image file specifying PNG format
				BufferedImage image = ImageIO.read(imageFile);

				// Create transferable object with image data
				Transferable transferable = new Transferable() {
					@Override
					public DataFlavor[] getTransferDataFlavors() {
						return new DataFlavor[]{DataFlavor.imageFlavor};
					}

					@Override
					public boolean isDataFlavorSupported(DataFlavor flavor) {
						return DataFlavor.imageFlavor.equals(flavor);
					}

					@Override
					public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
						if (!isDataFlavorSupported(flavor)) {
							throw new UnsupportedFlavorException(flavor);
						}
						return image;
					}
				};

				// Set the transferable object to clipboard
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);

				System.out.println("Image copied to clipboard successfully.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void openExplorerToFolder(String folderPath) {
		File folder = new File(folderPath);
		if (folder.exists() && folder.isDirectory()) {
			try {
				Desktop.getDesktop().open(folder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Folder does not exist or is not a directory: " + folderPath);
		}
	}
}


