package androidtoolkit.ui;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.BuildInstallRequest;
import androidtoolkit.app.BuildInstaller;
import androidtoolkit.app.BuildUninstallRequest;
import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.domain.BuildSelectionState;
import androidtoolkit.service.BuildSelectionStore;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.StoragePaths;

import java.awt.*;
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
import androidtoolkit.ui.components.*;

public class MyFrame extends JFrame implements PropertyChangeListener, BuildOperationCoordinator.BuildOperationUi, DeviceMonitor.DeviceMonitorUi, BuildSelectionCoordinator.BuildSelectionUi {

	private final AppServices appServices;
	private final CommandExecutor commandExecutor;
	private final StoragePaths storagePaths;
	private final BuildSelectionStore buildSelectionStore;
	File file1 = null;
	RefreshDevicesButton RefreshDevicesButton;
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
	BuildSelectionCoordinator buildSelectionCoordinator;
	DeviceCatalog deviceCatalog;
	DeviceMonitor deviceMonitor;
	DevicePanelFactory devicePanelFactory;
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
		this.buildSelectionCoordinator = new BuildSelectionCoordinator(buildSelectionStore);
		this.deviceCatalog = appServices.deviceCatalog();
		this.deviceMonitor = new DeviceMonitor(deviceCatalog, this::currentSerials, this, 3000);
		this.devicePanelFactory = new DevicePanelFactory(appServices);
		this.myFrameStateFactory = new MyFrameStateFactory();

		File logs = storagePaths.logsDir();
		if (!logs.exists()) {
			logs.mkdirs();
		}
		icon = new Icons();

		setStaticElements();
		refreshListOfDevices();
		this.setVisible(true);
		deviceMonitor.start();
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
		defaultBuildLocation.addActionListener(new DefaultBuildLocationAction(this));

		consoleViewMenu = new JMenuItem("Show Console View");
		toolsMenu.add(consoleViewMenu);
		consoleViewMenu.addActionListener(new ConsoleViewToggleAction(this));

		this.setJMenuBar(menuBar);

		DeviceDiscoveryResult discoveryResult = deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
		serialNumberList = discoveryResult.getSerials();
		numberOfDevices = discoveryResult.getDeviceCount();
		installButton = new InstallButton();
		installButton.addActionListener(new InstallSelectedDevicesAction(this));
		this.add(installButton);

		uninstallAllButton = new UninstallAllButton();
		uninstallAllButton.addActionListener(new UninstallAllDevicesAction(this));
		this.add(uninstallAllButton);

		staticPane = new StaticPane();
		this.add(staticPane);

		buildSelectionState = buildSelectionCoordinator.loadInitialState();
		System.out.println("Polazna lista buildova je : " + buildSelectionState.getBuildPaths());
		System.out.println("Polazna lista imena je : " + buildSelectionState.getBuildNames());

		fileTextFieldBox = new FileTextFieldBox(buildSelectionState.getBuildNames());
		fileTextFieldBox.addActionListener(new SelectBuildAction(this));
		this.add(fileTextFieldBox);

		RefreshDevicesButton = new RefreshDevicesButton(icon.display_icon);
		RefreshDevicesButton.addActionListener(new RefreshDevicesAction(this));
		this.add(RefreshDevicesButton);

		fileButton = new FileButton("Select Build");
		fileButton.addActionListener(new ChooseBuildAction(this));
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
		DeviceDiscoveryResult discoveryResult = deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
		java.util.List<ConnectedDevice> connectedDevices = discoveryResult.getDevices();
		serialNumberList = new ArrayList<>(connectedDevices.stream()
				.map(ConnectedDevice::getSerial)
				.collect(Collectors.toList()));
		numberOfDevices = discoveryResult.getDeviceCount();
		for (Device devicePanel : devicePanelFactory.createPanels(this, connectedDevices, this::refreshListOfDevices)) {
			device = devicePanel;
			listOfDevices.add(devicePanel);
			this.add(devicePanel);
			isInstalledList.add(devicePanel.appIsInstalled);
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

	private void updateDeviceList(ArrayList<String> tempListOfDevices) {
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

	@Override
	public void onDeviceListChanged(DeviceDiscoveryResult discoveryResult) {
		SwingUtilities.invokeLater(() -> {
			updateDeviceList(discoveryResult.getSerials());
			updatePanelSize();
		});
	}

	private ArrayList<String> currentSerials() {
		return new ArrayList<>(serialNumberList);
	}

	private void applyWindowSize() {
		if (consoleView != null && consoleView.isVisible()) {
			setSize(width, height + 200);
		} else {
			setSize(width, height);
		}
	}

	@Override
	public File chooseBuildFile(File defaultLocation) {
		JFileChooser fileChooser = new JFileChooser(defaultLocation.getAbsolutePath());
		int response = fileChooser.showOpenDialog(fileButton);
		if (response == JFileChooser.APPROVE_OPTION) {
			file1 = new File(fileChooser.getSelectedFile().getAbsolutePath());
			return file1;
		}
		return null;
	}

	@Override
	public File chooseDefaultBuildLocation() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int response = fileChooser.showOpenDialog(null);
		if (response == JFileChooser.APPROVE_OPTION) {
			return new File(fileChooser.getSelectedFile().getAbsolutePath());
		}
		return null;
	}

	@Override
	public void refreshBuildNames(java.util.List<String> buildNames) {
		buildSelectionState = buildSelectionCoordinator.currentState();
		fileTextFieldBox.removeAllItems();
		for (String buildName : buildNames) {
			fileTextFieldBox.addItem(buildName);
		}
	}

	@Override
	public void selectBuildIndex(int index) {
		fileTextFieldBox.setSelectedIndex(index);
	}

	@Override
	public void onBuildSelectionChanged() {
		buildSelectionState = buildSelectionCoordinator.currentState();
		applyFrameState(createFrameState());
		progressBar.setBackground(new Color(238, 238, 238));
		progressBar.setString("Waiting for build...");
	}

	@Override
	public void showDefaultBuildLocationSaved(File location) {
		JOptionPane.showMessageDialog(null, "Default build location is set!" + "\n" + location.getAbsolutePath(), "Default Build Location",
				JOptionPane.INFORMATION_MESSAGE);
	}

	void chooseDefaultBuildLocationSetting() {
		buildSelectionCoordinator.chooseDefaultBuildLocation(this);
	}

	void toggleConsoleView() {
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

	void showRefreshedDevices() {
		for (Device element : listOfDevices) {
			element.setVisible(false);
		}
		refreshListOfDevices();
		applyWindowSize();
		setVisible(true);
	}

	void startInstallSelectedDevices() {
		buildOperationCoordinator.startInstall(
				new BuildInstallRequest(
						listOfDevices.stream().map(Device::toDeviceTarget).collect(Collectors.toList()),
						buildSelectionState.getPrimaryBuildPath(),
						buildSelectionState.getPrimaryBuildName()
				),
				consoleView,
				this
		);
	}

	void startUninstallInstalledDevices() {
		buildOperationCoordinator.startUninstall(
				new BuildUninstallRequest(
						listOfDevices.stream().map(Device::toDeviceTarget).collect(Collectors.toList()),
						buildSelectionState.getPrimaryBuildName()
				),
				consoleView,
				this
		);
	}

	void chooseBuild() {
		buildSelectionCoordinator.chooseBuild(this);
		buildSelectionState = buildSelectionCoordinator.currentState();
		System.out.println("Lista buildova: " + buildSelectionState.getBuildPaths());
		System.out.println("Lista imena: " + buildSelectionState.getBuildNames());
	}

	void selectBuildFromDropdown() {
		int selectedIndex = fileTextFieldBox.getSelectedIndex();
		System.out.println(fileTextFieldBox.getSelectedIndex());
		buildSelectionCoordinator.selectBuildAt(selectedIndex, this);
		buildSelectionState = buildSelectionCoordinator.currentState();
		System.out.println(buildSelectionState.getBuildPaths());
		System.out.println(buildSelectionState.getBuildNames());
	}
}


