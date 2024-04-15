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
	ArrayList<String> serialNumberList = new ArrayList<>();
	Device device;
	public Icons icon;
	ArrayList<Device> listOfDevices = new ArrayList<>();
	ArrayList<Boolean> isInstalledList = new ArrayList<>();
	int numberOfDevices;
	FileTextFieldBox fileTextFieldBox;
	ArrayList<String> builds;
	ArrayList<String> build_names;
	ConsoleView consoleView;
	int height = 460;
	int width;
	Boolean isConsoleVisible = false;
	JMenuItem consoleViewMenu;
	static LiveEventTracker liveEventTracker;

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
		JMenu toolsMenu = new JMenu("Tools");
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(helpMenu);
		menuBar.add(toolsMenu);

		JMenuItem defaultBuildLocation = new JMenuItem("Set Default 'Select Build' Location");
		editMenu.add(defaultBuildLocation);
		defaultBuildLocation.addActionListener(new DefaultBuildLocationListener());

		consoleViewMenu = new JMenuItem("Show Console View");
		toolsMenu.add(consoleViewMenu);
		consoleViewMenu.addActionListener(new ConsoleViewActionListener());

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

		File temp_builds = new File("C:/AdbToolkit/builds.ser");
		if (temp_builds.exists()) {
			try {
				ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(temp_builds));
				builds = (ArrayList<String>) objectInputStream.readObject();
				objectInputStream.close();
				System.out.println("Polazna lista buildova je : " + builds);
				build_names = new ArrayList<>();
				for (int i = 0; i < builds.size(); i++) {
					if (builds.get(i) != null) {
						String separator = builds.get(i).contains("/") ? "/" : "\\"; // Check if path uses '/' or '\'
						build_names.add(builds.get(i).substring(builds.get(i).lastIndexOf(separator) + 1));
					}
				}
				System.out.println("Polazna lista imena je : " + build_names);
			} catch (IOException | ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
			try {
				FileOutputStream fs = new FileOutputStream("C:/AdbToolkit/builds.ser");
				ObjectOutputStream os = new ObjectOutputStream(fs);
				os.writeObject(builds);
				os.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		} else{
			builds = new ArrayList<>();
			build_names = new ArrayList<>();
		}

		fileTextFieldBox = new FileTextFieldBox(build_names);
		fileTextFieldBox.addActionListener(new FileButtonBoxListener());
		this.add(fileTextFieldBox);

		devicesButton = new DevicesButton(icon.display_icon);
		devicesButton.addActionListener(new DevicesButtonListener());
		this.add(devicesButton);

		fileButton = new FileButton("Select Build");
		fileButton.addActionListener(new FileButtonListener());
		this.add(fileButton);

		progressBar = new ProgressBar();
		this.add(progressBar);

		width = numberOfDevices * 210 + 230;
		consoleView = new ConsoleView(this);
		this.add(consoleView);
		this.setTitle("Adb Toolkit");
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setLayout(null);
		this.setResizable(false);

		this.setMinimumSize(new Dimension(650, 460));
		this.setSize(width, height);
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
		if(isInstalledList.contains(false) && !fileTextFieldBox.getItemAt(0).equals("")){
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

	class ConsoleViewActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!consoleView.isVisible()) {
				consoleView.setVisible(true);
				isConsoleVisible = true;
				setSize(width, height + 200);
				consoleViewMenu.setText("Hide Console View");
			} else {
				consoleView.setVisible(false);
				isConsoleVisible = false;
				consoleViewMenu.setText("Show Console View");
				setSize(width, height);
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
			setSize(width, height);
			setVisible(true);
		}
	}

	class InstallButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			progressBar.setString("Installing...");
			progressBar.setIndeterminate(true);
			progressBar.setBackground(Color.WHITE);
			installButton.setEnabled(false);
			for (int i = 0; i < numberOfDevices; i++) {
				if (listOfDevices.get(i).radio.isSelected() && !listOfDevices.isEmpty()) {
					listOfDevices.get(i).radioState = true;
					Task task1 = new Task("adb -s " + listOfDevices.get(i).serial + " install " + "\"" + builds.get(0) + "\"");
					task1.addPropertyChangeListener(null);
					task1.execute();
					consoleView.appendText(listOfDevices.get(i).deviceName + " (" + listOfDevices.get(i).serial + "):" + "\n" + "App installed: " + build_names.get(0));
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
				if(listOfDevices.get(i).appIsInstalled) {
					task[i] = new Task("adb -s " + listOfDevices.get(i).serial + " shell pm uninstall "
							+ utility.getSafePathPackage(listOfDevices.get(i).serial));
					task[i].addPropertyChangeListener(null);
					task[i].execute();
					consoleView.appendText(listOfDevices.get(i).deviceName + " (" + listOfDevices.get(i).serial + "):" + "\n" + "App removed: " + build_names.get(0));
				}
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
				builds.add(0, file1.getAbsolutePath());

				fileTextFieldBox.insertItemAt(file1.getName(), 0);
				fileTextFieldBox.setSelectedIndex(0);
				if (builds.size() > 5) {
					builds.remove(5);
				}
				System.out.println("Lista buildova: " + builds);
				System.out.println(builds.size());
				try {
					FileOutputStream fs = new FileOutputStream("C:/AdbToolkit/builds.ser");
					ObjectOutputStream os = new ObjectOutputStream(fs);
					os.writeObject(builds);
					os.close();
				} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				if (!serialNumberList.isEmpty() && isInstalledList.contains(false)){
					installButton.setEnabled(true);
				}
				progressBar.setBackground(new Color(238, 238, 238));
				progressBar.setString("Waiting for build...");
			}
		}
	}

	class FileButtonBoxListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == fileTextFieldBox) {
				//Swap current build at index 0
				int temp_index = fileTextFieldBox.getSelectedIndex();
				String temp = builds.get(temp_index);
				builds.set(temp_index, builds.get(0));
				builds.set(0, temp);
				System.out.println(builds);
				try {
					FileOutputStream fs = new FileOutputStream("C:/AdbToolkit/builds.ser");
					ObjectOutputStream os = new ObjectOutputStream(fs);
					os.writeObject(builds);
					os.close();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
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
				if (isInstalledList.contains(false) && !fileTextFieldBox.getItemAt(0).equals("")) {
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
			width = numberOfDevices * 210 + 230;
			if (consoleView.isVisible()) {
				setSize(width, height + 200);
			} else {
				setSize(width, height);
			}
			revalidate();
			repaint();
		});
	}
}