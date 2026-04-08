package androidtoolkit.ui;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.BuildInstallRequest;
import androidtoolkit.app.BuildUninstallRequest;
import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.domain.BuildSelectionState;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class MyFrame extends JFrame implements PropertyChangeListener, BuildOperationCoordinator.BuildOperationUi, DeviceMonitor.DeviceMonitorUi, BuildSelectionCoordinator.BuildSelectionUi {

	private static final int BASE_HEIGHT = 500;
	private static final int CONSOLE_HEIGHT_DELTA = 200;
	private final DevicePanelCollection devicePanelCollection;
	private final BuildOperationCoordinator buildOperationCoordinator;
	private final BuildSelectionCoordinator buildSelectionCoordinator;
	private final DeviceCatalog deviceCatalog;
	private final MyFrameStateFactory myFrameStateFactory;
	private final MyFrameInitializer initializer;
	private final MyFrameUiSupport uiSupport;
	public final Icons icon;
	private final MyFrameComponents components;
	private int width;

	public MyFrame() {
		this(new AppServices());
	}

	public MyFrame(AppServices appServices) {
		this.buildOperationCoordinator = new BuildOperationCoordinator(appServices.buildInstaller());
		this.buildSelectionCoordinator = new BuildSelectionCoordinator(appServices.buildSelectionStore());
		this.deviceCatalog = appServices.deviceCatalog();
		DevicePanelFactory devicePanelFactory = new DevicePanelFactory(appServices);
		this.devicePanelCollection = new DevicePanelCollection(this.getContentPane(), devicePanelFactory, this::refreshDevices);
		DeviceMonitor deviceMonitor = new DeviceMonitor(deviceCatalog, devicePanelCollection::currentSerials, this, 3000);
		this.myFrameStateFactory = new MyFrameStateFactory();
		this.initializer = new MyFrameInitializer();
		this.uiSupport = new MyFrameUiSupport();

		File logs = appServices.storagePaths().logsDir();
		if (!logs.exists()) {
			logs.mkdirs();
		}
		icon = new Icons();

		BuildSelectionState initialBuildSelectionState = buildSelectionCoordinator.loadInitialState();
		components = new MyFrameComponents(this, icon, initialBuildSelectionState, appServices.commandExecutor());
		initializeFrameUi();
		refreshDevices();
		this.setVisible(true);
		deviceMonitor.start();
	}

	private void initializeFrameUi() {
		initializer.initialize(this, components, uiSupport, icon, BASE_HEIGHT);
		applyCurrentFrameState();
	}

	public void refreshListOfDevices() {
		refreshDevicePanels(deviceCatalog.discoverDevices(new DeviceDiscoveryRequest()));
	}

	private void refreshDevicePanels(DeviceDiscoveryResult discoveryResult) {
		devicePanelCollection.replace(this, discoveryResult);
		applyCurrentFrameState();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			int progress = (Integer) evt.getNewValue();
			components.progressBar().setIndeterminate(false);
			components.progressBar().setValue(progress);
		}
	}

	private MyFrameState createFrameState() {
		return myFrameStateFactory.create(
				devicePanelCollection.deviceCount(),
				currentBuildSelectionState().hasBuilds(),
				devicePanelCollection.hasInstallableDevice(),
				devicePanelCollection.hasInstalledDevice()
		);
	}

	private void applyFrameState(MyFrameState frameState) {
		components.installButton().setEnabled(frameState.isInstallEnabled());
		components.uninstallAllButton().setEnabled(frameState.isUninstallAllEnabled());
		width = frameState.getWindowWidth();
		applyWindowSize();
	}

	@Override
	public void applyProgressState(BuildProgressState progressState) {
		components.progressBar().setString(progressState.getMessage());
		components.progressBar().setIndeterminate(progressState.isIndeterminate());
		components.progressBar().setBackground(progressState.getBackgroundColor());
	}

	@Override
	public void setInstallEnabled(boolean enabled) {
		components.installButton().setEnabled(enabled);
	}

	@Override
	public void setUninstallAllEnabled(boolean enabled) {
		components.uninstallAllButton().setEnabled(enabled);
	}

	@Override
	public void refreshDevices() {
		refreshDisplayedDevices(deviceCatalog.discoverDevices(new DeviceDiscoveryRequest()));
	}

	@Override
	public void refreshDevicesAfterBuildOperation() {
		refreshDevices();
		// Package visibility can lag slightly after adb install/uninstall, especially with multiple devices.
		Timer timer = new Timer(1000, event -> refreshDevices());
		timer.setRepeats(false);
		timer.start();
	}

	@Override
	public void applyCurrentFrameState() {
		applyFrameState(createFrameState());
	}

	@Override
	public void onDeviceListChanged(DeviceDiscoveryResult discoveryResult) {
		SwingUtilities.invokeLater(() -> refreshDisplayedDevices(discoveryResult));
	}

	private void applyWindowSize() {
		if (isConsoleVisible()) {
			setSize(width, BASE_HEIGHT + CONSOLE_HEIGHT_DELTA);
		} else {
			setSize(width, BASE_HEIGHT);
		}
	}

	@Override
	public File chooseBuildFile(File defaultLocation) {
		return uiSupport.chooseBuildFile(this, defaultLocation, components.fileButton());
	}

	@Override
	public File chooseDefaultBuildLocation() {
		return uiSupport.chooseDirectory(this);
	}

	@Override
	public void refreshBuildNames(java.util.List<String> buildNames) {
		replaceBuildNames(buildNames);
	}

	@Override
	public void selectBuildIndex(int index) {
		components.fileTextFieldBox().setSelectedIndex(index);
	}

	@Override
	public void onBuildSelectionChanged() {
		applyCurrentFrameState();
		resetProgressToWaiting();
	}

	@Override
	public void showDefaultBuildLocationSaved(File location) {
		uiSupport.showInfoMessage(this, "Default build location is set!" + "\n" + location.getAbsolutePath(), "Default Build Location");
	}

	void chooseDefaultBuildLocationSetting() {
		buildSelectionCoordinator.chooseDefaultBuildLocation(this);
	}

	void toggleConsoleView() {
		boolean visible = !isConsoleVisible();
		components.consoleView().setVisible(visible);
		components.consoleViewMenu().setText(visible ? "Hide Console View" : "Show Console View");
		applyWindowSize();
	}

	boolean isConsoleVisible() {
		return components.consoleView().isVisible();
	}

	void appendConsoleText(String text) {
		components.consoleView().appendText(text);
	}

	void showRefreshedDevices() {
		refreshDevices();
	}

	void startInstallSelectedDevices() {
		BuildSelectionState buildSelectionState = currentBuildSelectionState();
		buildOperationCoordinator.startInstall(
				new BuildInstallRequest(
						devicePanelCollection.panels().stream().map(Device::toDeviceTarget).collect(Collectors.toList()),
						buildSelectionState.getPrimaryBuildPath(),
						buildSelectionState.getPrimaryBuildName()
				),
				components.consoleView(),
				this
		);
	}

	void startUninstallInstalledDevices() {
		BuildSelectionState buildSelectionState = currentBuildSelectionState();
		buildOperationCoordinator.startUninstall(
				new BuildUninstallRequest(
						devicePanelCollection.panels().stream().map(Device::toDeviceTarget).collect(Collectors.toList()),
						buildSelectionState.getPrimaryBuildName()
				),
				components.consoleView(),
				this
		);
	}

	void chooseBuild() {
		buildSelectionCoordinator.chooseBuild(this);
	}

	void selectBuildFromDropdown() {
		int selectedIndex = components.fileTextFieldBox().getSelectedIndex();
		buildSelectionCoordinator.selectBuildAt(selectedIndex, this);
	}

	private BuildSelectionState currentBuildSelectionState() {
		return buildSelectionCoordinator.currentState();
	}

	private void refreshDisplayedDevices(DeviceDiscoveryResult discoveryResult) {
		refreshDevicePanels(discoveryResult);
		refreshFrameDisplay();
	}

	private void refreshFrameDisplay() {
		revalidate();
		repaint();
	}

	private void replaceBuildNames(java.util.List<String> buildNames) {
		components.fileTextFieldBox().removeAllItems();
		for (String buildName : buildNames) {
			components.fileTextFieldBox().addItem(buildName);
		}
	}

	private void resetProgressToWaiting() {
		components.progressBar().setBackground(new Color(238, 238, 238));
		components.progressBar().setString("Waiting for build...");
	}
}


