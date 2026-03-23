package androidtoolkit.ui;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.BuildInstallRequest;
import androidtoolkit.app.BuildInstaller;
import androidtoolkit.app.BuildUninstallRequest;
import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.domain.BuildSelectionState;
import androidtoolkit.service.BuildSelectionStore;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.StoragePaths;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javax.swing.*;
import Buttons.*;

public class MyFrame extends JFrame implements PropertyChangeListener {

	private final AppServices appServices;
	private final CommandExecutor commandExecutor;
	private final StoragePaths storagePaths;
	private final BuildSelectionStore buildSelectionStore;
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
	BuildSelectionState buildSelectionState;
	ConsoleView consoleView;
	BuildInstaller buildInstaller;
	DeviceCatalog deviceCatalog;
	int height = 460;
	int width;
	Boolean isConsoleVisible = false;
	JMenuItem consoleViewMenu;
	static LiveEventTracker liveEventTracker;

	public MyFrame() {
		this(new AppServices());
	}

	public MyFrame(AppServices appServices) {
		this.appServices = appServices;
		this.commandExecutor = appServices.commandExecutor();
		this.storagePaths = appServices.storagePaths();
		this.buildSelectionStore = appServices.buildSelectionStore();
		this.buildInstaller = appServices.buildInstaller();
		this.deviceCatalog = appServices.deviceCatalog();

		File logs = storagePaths.logsDir();
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

		serialNumberList = deviceCatalog.connectedSerials();
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

		buildSelectionState = buildSelectionStore.loadBuildSelection();
		System.out.println("Polazna lista buildova je : " + buildSelectionState.getBuildPaths());
		System.out.println("Polazna lista imena je : " + buildSelectionState.getBuildNames());

		fileTextFieldBox = new FileTextFieldBox(buildSelectionState.getBuildNames());
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
		consoleView = new ConsoleView(this, commandExecutor);
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
		isInstalledList.clear();
		java.util.List<ConnectedDevice> connectedDevices = deviceCatalog.loadConnectedDevices();
		serialNumberList = new ArrayList<>(connectedDevices.stream()
				.map(ConnectedDevice::getSerial)
				.collect(Collectors.toList()));
		numberOfDevices = connectedDevices.size();
		for (ConnectedDevice connectedDevice : connectedDevices) {
			device = new Device(this, connectedDevice, numberOfDevices, this::refreshListOfDevices, appServices);
			device.setVisible(true);
			listOfDevices.add(device);
			this.add(device);
			isInstalledList.add(device.appIsInstalled);
		}
		if(isInstalledList.contains(false) && fileTextFieldBox.getItemCount() > 0 && !fileTextFieldBox.getItemAt(0).equals("")){
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

	class DefaultBuildLocationListener implements ActionListener {

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
				buildSelectionStore.saveDefaultBuildLocation(file2);
				JOptionPane.showMessageDialog(null, "Default build location is set!" + "\n" + file2.getAbsolutePath(), "Default Build Location",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (RuntimeException ex) {
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
			if (consoleView.isVisible()) {
				setSize(width, height + 200);
			} else {
				setSize(width, height);
			}
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
			int tasksStarted = buildInstaller.installSelectedDevices(
					new BuildInstallRequest(
							listOfDevices.stream().map(Device::toDeviceTarget).collect(Collectors.toList()),
							buildSelectionState.getPrimaryBuildPath(),
							buildSelectionState.getPrimaryBuildName()
					),
					consoleView,
					MyFrame.this::startTask
			);
			if (tasksStarted > 0) {
				uninstallAllButton.setEnabled(true);
			} else {
				progressBar.setIndeterminate(false);
				progressBar.setString("Waiting for build...");
				installButton.setEnabled(true);
			}
		}
	}

	class UninstallAllButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			progressBar.setString("Uninstalling...");
			progressBar.setIndeterminate(true);
			progressBar.setBackground(new Color(238, 238, 238));
			uninstallAllButton.setEnabled(false);
			int tasksStarted = buildInstaller.uninstallInstalledDevices(
					new BuildUninstallRequest(
							listOfDevices.stream().map(Device::toDeviceTarget).collect(Collectors.toList()),
							buildSelectionState.getPrimaryBuildName()
					),
					consoleView,
					MyFrame.this::startTask
			);
			if (tasksStarted == 0) {
				progressBar.setIndeterminate(false);
				progressBar.setString("Done!");
				uninstallAllButton.setEnabled(true);
			}
		}
	}

	class FileButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			File default_location = buildSelectionStore.loadDefaultBuildLocation();
			JFileChooser fileChooser = new JFileChooser(default_location.getAbsolutePath());
			int response = fileChooser.showOpenDialog(fileButton);
			if (response == JFileChooser.APPROVE_OPTION) {
				file1 = new File(fileChooser.getSelectedFile().getAbsolutePath());
				String build = file1.getAbsolutePath();
				buildSelectionState.addBuild(build);
				refreshBuildSelectionBox();
				fileTextFieldBox.setSelectedIndex(0);
				System.out.println("Lista buildova: " + buildSelectionState.getBuildPaths());
				System.out.println("Lista imena: " + buildSelectionState.getBuildNames());
				buildSelectionStore.saveBuildSelection(buildSelectionState);
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
				int temp_index = fileTextFieldBox.getSelectedIndex();
				System.out.println(fileTextFieldBox.getSelectedIndex());
				buildSelectionState.selectBuild(temp_index);
				refreshBuildSelectionBox();
				buildSelectionStore.saveBuildSelection(buildSelectionState);
				System.out.println(buildSelectionState.getBuildPaths());
				System.out.println(buildSelectionState.getBuildNames());
			}
		}
	}

	class Task extends SwingWorker<Void, Void> {
		private final String command;

		public Task(String command) {
			this.command = command;
		}

		@Override
		public Void doInBackground() {
			buildInstaller.runCommand(command);
			return null;
		}

		@Override
		public void done() {
			if (buildInstaller.finishTask() == 0) {
				progressBar.setString("Done!");
				progressBar.setBackground(Color.green);
				refreshListOfDevices();
				if (isInstalledList.contains(false) && fileTextFieldBox.getItemCount() > 0 && !fileTextFieldBox.getItemAt(0).equals("")) {
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
					ArrayList<String> tempSerialNumberList = deviceCatalog.connectedSerials();
					if (!tempSerialNumberList.equals(serialNumberList)) {
						updateDeviceList(tempSerialNumberList);
						updatePanelSize();
					}
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
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

	private void refreshBuildSelectionBox() {
		fileTextFieldBox.removeAllItems();
		for (String buildName : buildSelectionState.getBuildNames()) {
			fileTextFieldBox.addItem(buildName);
		}
	}

	private void startTask(String command) {
		Task task = new Task(command);
		task.addPropertyChangeListener(null);
		task.execute();
	}
}
