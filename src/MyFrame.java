import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.ArrayList;
import javax.swing.*;
import Buttons.*;

public class MyFrame extends JFrame implements PropertyChangeListener {

	static Util utility = new Util();
	private static int counter = 0;
	private static final Object lock = new Object();
	File file1 = null;
	DevicesButton devicesButton;
	JButton fileButton;
	InstallButton installButton;
	UninstallAllButton uninstallAllButton;
	JTextPane staticPane;
	ProgressBar progressBar;
	FileTextField fileTextField;
	ArrayList<String> serialNumberList = new ArrayList<>();
	Device device;
	public Icons icon;
	ArrayList<Device> listOfDevices = new ArrayList<>();
	ArrayList<Boolean> isInstalledList = new ArrayList<>();
	int numberOfDevices;

	public MyFrame() {

		File logs = new File("C:/AdbToolkit/Logs");
		if (!logs.exists()) {
			logs.mkdirs();
		}
		icon = new Icons();

		setStaticElements();
		refreshListOfDevices();
		this.setVisible(true);
		startDeviceCheckThread();
	}

	private void setStaticElements() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu editMenu = new JMenu("Edit");
		JMenu helpMenu = new JMenu("Help");
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(helpMenu);
		JMenuItem defaultBuildLocation = new JMenuItem("Set Default 'Select Build' Location");
		editMenu.add(defaultBuildLocation);
		defaultBuildLocation.addActionListener(new DefaultBuildLocationListener());
		this.setJMenuBar(menuBar);
		serialNumberList = utility.getConnectedDevices();
		numberOfDevices = serialNumberList.size();
		installButton = new InstallButton();
		installButton.addActionListener(new InstallButtonListener());
		this.add(installButton);

		uninstallAllButton = new UninstallAllButton();
		uninstallAllButton.addActionListener(new UninstallAllButtonListener());
		int j = 0;
		while (j < numberOfDevices) {
			if (!isInstalledList.contains(true)) {
				uninstallAllButton.setEnabled(true);
				break;
			}
			j++;
		}
		this.add(uninstallAllButton);

		staticPane = new StaticPane();
		this.add(staticPane);
		fileTextField = new FileTextField();
		this.add(fileTextField);

		devicesButton = new DevicesButton(icon.display_icon);
		devicesButton.addActionListener(new DevicesButtonListener());
		this.add(devicesButton);

		fileButton = new FileButton("Select Build");
		fileButton.addActionListener(new FileButtonListener());
		this.add(fileButton);

		progressBar = new ProgressBar();
		this.add(progressBar);

		this.setTitle("Adb Toolkit");
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setLayout(null);
		this.setResizable(false);
		int width = numberOfDevices * 210 + 230;
		this.setMinimumSize(new Dimension(650, 450));
		this.setSize(width, 450);
		this.setIconImage(icon.frameIcon.getImage());
	}

	public void refreshListOfDevices() {
		for (Device element : listOfDevices) {
			element.setVisible(false);
		}
		listOfDevices.clear();
		serialNumberList = utility.getConnectedDevices();
		for (int i = 0; i < serialNumberList.size(); i++) {
			device = new Device(this, i, this::refreshListOfDevices);
			device.setVisible(true);
			listOfDevices.add(device);
			this.add(device);
			isInstalledList.add(device.appIsInstalled);
			numberOfDevices = listOfDevices.size();
		}
		if(isInstalledList.contains(false) && !fileTextField.getText().isEmpty()){
			installButton.setEnabled(true);
		}
		if(!isInstalledList.contains(true)){
			uninstallAllButton.setEnabled(false);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			int progress = (Integer) evt.getNewValue();
			progressBar.setIndeterminate(false);
			progressBar.setValue(progress);
		}
	}

	static class DefaultBuildLocationListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int response = fileChooser.showOpenDialog(null);
			File file2 = null;
			if (response == JFileChooser.APPROVE_OPTION) {
				file2 = new File(fileChooser.getSelectedFile().getAbsolutePath());
			}
			try {
				FileOutputStream fs = new FileOutputStream("C:/AdbToolkit/location.ser");
				ObjectOutputStream os = new ObjectOutputStream(fs);
				os.writeObject(file2);
				os.close();
				JOptionPane.showMessageDialog(null, "Default build location is set!" + "\n" + file2.getAbsolutePath(), "Default Build Location",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	class DevicesButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			for (Device element : listOfDevices) {
				element.setVisible(false);
			}
			refreshListOfDevices();
			int width = numberOfDevices * 210 + 230;
			setSize(width, 420);
			setVisible(true);
		}
	}

	class InstallButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			progressBar.setString("Installing...");
			progressBar.setIndeterminate(true);
			progressBar.setBackground(new Color(238, 238, 238));
			installButton.setEnabled(false);
			for (int i = 0; i < numberOfDevices; i++) {
				if (listOfDevices.get(i).radio.isSelected() && !listOfDevices.isEmpty()) {
					listOfDevices.get(i).radioState = true;
					Task task1 = new Task("adb -s " + listOfDevices.get(i).serial + " install " + "\"" + file1.getAbsolutePath() + "\"");
					task1.addPropertyChangeListener(null);
					task1.execute();
				} else {
					listOfDevices.get(i).radioState = false;
				}
			}
			uninstallAllButton.setEnabled(true);
		}
	}

	class UninstallAllButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			progressBar.setString("Uninstalling...");
			progressBar.setIndeterminate(true);
			progressBar.setBackground(new Color(238, 238, 238));
			uninstallAllButton.setEnabled(false);
			int i = 0;
			Task[] task = new Task[numberOfDevices];
			while (i < numberOfDevices) {
				task[i] = new Task("adb -s " + listOfDevices.get(i).serial + " shell pm uninstall "
						+ utility.getSafePathPackage(listOfDevices.get(i).serial));
				task[i].addPropertyChangeListener(null);
				task[i].execute();
				i++;
			}
		}
	}

	class FileButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			File default_location = new File("C:/AdbToolkit/location.ser");
			if (default_location.exists()) {
				try {
					ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("C:/AdbToolkit/location.ser"));
					default_location = (File) objectInputStream.readObject();
				} catch (IOException | ClassNotFoundException ex) {
					throw new RuntimeException(ex);
				}
			}
			JFileChooser fileChooser = new JFileChooser(default_location.getAbsolutePath());
			int response = fileChooser.showOpenDialog(fileButton);
			if (response == JFileChooser.APPROVE_OPTION) {
				file1 = new File(fileChooser.getSelectedFile().getAbsolutePath());
				fileTextField.setText(file1.getName());
				if (!serialNumberList.isEmpty() && isInstalledList.contains(false)){
					installButton.setEnabled(true);
				}
				progressBar.setBackground(new Color(238, 238, 238));
				progressBar.setString("Waiting for build...");
			}
		}
	}

	// Thread synchronization to determine the end of all processes
	public void increaseCounter() {
		synchronized (lock) {
			counter++;
		}
	}

	public void decreaseCounter() {
		synchronized (lock) {
			counter--;
		}
	}

	class Task extends SwingWorker<Void, Void> {
		private final String command;

		public Task(String command) {
			this.command = command;
		}

		@Override
		public Void doInBackground() {
			increaseCounter();
			utility.runCommand(command);
			return null;
		}

		@Override
		public void done() {
			decreaseCounter();
			if (counter == 0) {
				progressBar.setString("Done!");
				progressBar.setBackground(Color.green);
				refreshListOfDevices();
				if (isInstalledList.contains(false) && !fileTextField.getText().isEmpty()) {
					installButton.setEnabled(true);
				}
				progressBar.setIndeterminate(false);
				uninstallAllButton.setEnabled(true);
			}
		}
	}

	public void startDeviceCheckThread() {
		Thread thread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					ArrayList<String> tempSerialNumberList = utility.getConnectedDevices();
					if (!tempSerialNumberList.equals(serialNumberList)) {
						updateDeviceList(tempSerialNumberList);
						updatePanelSize();
					}
					Thread.sleep(3000); // Sleep for 3 seconds
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt(); // Restore interrupted status
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	public void updateDeviceList(ArrayList<String> tempListOfDevices) {
		for(Device element : listOfDevices) {
			remove(element);
		}
		refreshListOfDevices();
		for(Device element : listOfDevices) {
			element.setVisible(true);
		}
		serialNumberList = tempListOfDevices;
	}

	private void updatePanelSize() {
		SwingUtilities.invokeLater(() -> {
			int width = numberOfDevices * 210 + 230;
			setSize(width, 420);
			revalidate();
			repaint();
		});
	}
}