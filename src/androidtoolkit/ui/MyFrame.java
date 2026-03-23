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
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import Buttons.*;

public class MyFrame extends JFrame implements PropertyChangeListener, BuildOperationCoordinator.BuildOperationUi {

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
	BuildOperationCoordinator buildOperationCoordinator;
	DeviceCatalog deviceCatalog;
	MyFrameStateFactory myFrameStateFactory;
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
		this.buildOperationCoordinator = new BuildOperationCoordinator(buildInstaller);
		this.deviceCatalog = appServices.deviceCatalog();
		this.myFrameStateFactory = new MyFrameStateFactory();

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

		consoleView = new ConsoleView(this, commandExecutor);
		this.add(consoleView);
		this.setTitle("Adb Toolkit");
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setLayout(null);
		this.setResizable(false);

		this.setMinimumSize(new Dimension(650, 460));
		applyFrameState(createFrameState());
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
		applyFrameState(createFrameState());
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
				consoleViewMenu.setText("Hide Console View");
			} else {
				consoleView.setVisible(false);
				isConsoleVisible = false;
				consoleViewMenu.setText("Show Console View");
			}
			applyWindowSize();
		}
	}

	class DevicesButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			for (Device element : listOfDevices) {
				element.setVisible(false);
			}
			refreshListOfDevices();
			applyWindowSize();
			setVisible(true);
		}
	}

	class InstallButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			buildOperationCoordinator.startInstall(
					new BuildInstallRequest(
							listOfDevices.stream().map(Device::toDeviceTarget).collect(Collectors.toList()),
							buildSelectionState.getPrimaryBuildPath(),
							buildSelectionState.getPrimaryBuildName()
					),
					consoleView,
					MyFrame.this
			);
		}
	}

	class UninstallAllButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			buildOperationCoordinator.startUninstall(
					new BuildUninstallRequest(
							listOfDevices.stream().map(Device::toDeviceTarget).collect(Collectors.toList()),
							buildSelectionState.getPrimaryBuildName()
					),
					consoleView,
					MyFrame.this
			);
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
				applyFrameState(createFrameState());
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
			applyFrameState(createFrameState());
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

	private MyFrameState createFrameState() {
		return myFrameStateFactory.create(
				numberOfDevices,
				buildSelectionState != null && buildSelectionState.hasBuilds(),
				isInstalledList.contains(false),
				isInstalledList.contains(true)
		);
	}

	private void applyFrameState(MyFrameState frameState) {
		installButton.setEnabled(frameState.isInstallEnabled());
		uninstallAllButton.setEnabled(frameState.isUninstallAllEnabled());
		width = frameState.getWindowWidth();
		applyWindowSize();
	}

	@Override
	public void applyProgressState(BuildProgressState progressState) {
		progressBar.setString(progressState.getMessage());
		progressBar.setIndeterminate(progressState.isIndeterminate());
		progressBar.setBackground(progressState.getBackgroundColor());
	}

	@Override
	public void setInstallEnabled(boolean enabled) {
		installButton.setEnabled(enabled);
	}

	@Override
	public void setUninstallAllEnabled(boolean enabled) {
		uninstallAllButton.setEnabled(enabled);
	}

	@Override
	public void refreshDevices() {
		refreshListOfDevices();
	}

	@Override
	public void applyCurrentFrameState() {
		applyFrameState(createFrameState());
	}

	private void applyWindowSize() {
		if (consoleView != null && consoleView.isVisible()) {
			setSize(width, height + 200);
		} else {
			setSize(width, height);
		}
	}
}
