package androidtoolkit.ui;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceTarget;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.DeviceOperations;
import androidtoolkit.app.FirebaseDebugRequest;
import androidtoolkit.app.LogExportRequest;
import androidtoolkit.app.LogExporter;
import androidtoolkit.app.RebootDeviceRequest;
import androidtoolkit.app.ScreenMirrorRequest;
import androidtoolkit.app.ScreenshotRequest;
import androidtoolkit.app.ScreenshotResult;
import androidtoolkit.app.StartScreenRecordingRequest;
import androidtoolkit.app.StartScreenRecordingResult;
import androidtoolkit.app.StopScreenRecordingRequest;
import androidtoolkit.app.StopScreenRecordingResult;
import androidtoolkit.app.UninstallAppRequest;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugRequest;
import androidtoolkit.app.WifiDebugResult;
import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.domain.RecordingSession;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.DeviceActionService;
import androidtoolkit.service.DeviceGateway;
import androidtoolkit.service.DeviceInfoService;
import androidtoolkit.service.ScreenRecordingService;
import androidtoolkit.service.StoragePaths;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import androidtoolkit.ui.components.*;

public class Device extends JPanel {

    private final StoragePaths storagePaths;
    private final DeviceGateway deviceGateway;
    private final DeviceInfoService deviceInfoService;
    private final CommandExecutor commandExecutor;
    DeviceActionService deviceActionService;
    ScreenRecordingService screenRecordingService;
    DeviceOperations deviceOperations;
    LogExporter logExporter;
    DevicePanelStateFactory devicePanelStateFactory;
    File file = null;
    SaveSpLogsButton saveLogsButton;
    LogLocationButton logLocationButton;
    WifiDebugButton wifiDebug;
    EnableFirebaseButton enableFirebase;
    RebootButton reboot;
    TakeScreenshotButton takeScreenshotButton;
    UninstallAppButton uninstallApp;
    DeviceSelectionRadioButton radio;
    boolean radioState = false;
    LogoIconLabels labelIcon;
    DeviceTextPanes deviceTextPane;
    DeviceInfo deviceInfo;
    Icons icon;
    int totalDeviceCount;
    String serial;
    Boolean appIsInstalled;
    String deviceName;
    MyFrame parent;
    Runnable refreshDevicesMethod;
    String logLocation;
    String recordingLocation;
    EventTrackerButton eventTrackerButton;
    ScreenMirrorButton screenMirrorButton;
    ScreenRecordingButton screenRecordingButton;
    private final RecordingSession recordingSession = new RecordingSession();
    LiveEventTracker liveEventTracker;

    public Device(MyFrame parent, ConnectedDevice connectedDevice, int totalDeviceCount, Runnable refreshDevicesMethod, AppServices appServices) {

        this.setBounds((connectedDevice.getIndex()+1)*210, 0, 210, 310);
        this.setLayout(null);
        this.totalDeviceCount = totalDeviceCount;
        this.storagePaths = appServices.storagePaths();
        this.deviceGateway = appServices.deviceGateway();
        this.deviceInfoService = appServices.deviceInfoService();
        this.commandExecutor = appServices.commandExecutor();
        this.logLocation = storagePaths.logsDir().getPath();
        this.recordingLocation = storagePaths.screenRecordingsDir().getPath();
        icon = new Icons();
        deviceActionService = appServices.deviceActionService();
        screenRecordingService = appServices.screenRecordingService();
        deviceOperations = appServices.deviceOperations();
        logExporter = appServices.logExporter();
        devicePanelStateFactory = new DevicePanelStateFactory();
        serial = connectedDevice.getSerial();
        deviceName = connectedDevice.getDeviceName();
        deviceInfo = connectedDevice.getDeviceInfo();
        appIsInstalled = deviceInfo.isAppInstalled();
        setIconAndButtons(totalDeviceCount);
        this.setVisible(false);
        this.parent = parent;
        this.refreshDevicesMethod = refreshDevicesMethod;
    }

    private void setIconAndButtons(int totalDeviceCount) {
        radio = new DeviceSelectionRadioButton(deviceName);
        radio.setVisible(true);
        this.add(radio);

        labelIcon = new LogoIconLabels(icon.notInstalled);
        labelIcon.setVisible(true);
        this.add(labelIcon);

        deviceTextPane = new DeviceTextPanes();
        deviceTextPane.setText(deviceInfo.toDisplayText());
        deviceTextPane.setVisible(true);
        this.add(deviceTextPane);

        eventTrackerButton = new EventTrackerButton();
        eventTrackerButton.addActionListener(new DeviceEventTrackerAction(this));
        this.add(eventTrackerButton);

        saveLogsButton = new SaveSpLogsButton();
        saveLogsButton.addActionListener(new DeviceSaveLogsAction(this));
        this.add(saveLogsButton);

        logLocationButton = new LogLocationButton();
        logLocationButton.addActionListener(new DeviceLogLocationAction(this));
        this.add(logLocationButton);

        screenMirrorButton = new ScreenMirrorButton();
        screenMirrorButton.addActionListener(new DeviceScreenMirrorAction(this));
        this.add(screenMirrorButton);

        screenRecordingButton = new ScreenRecordingButton();
        screenRecordingButton.addActionListener(new DeviceScreenRecordingAction(this));
        this.add(screenRecordingButton);

        wifiDebug = new WifiDebugButton();
        wifiDebug.addActionListener(new DeviceToggleWifiDebugAction(this));
        this.add(wifiDebug);

        enableFirebase = new EnableFirebaseButton();
        enableFirebase.addActionListener(new DeviceEnableFirebaseAction(this));
        this.add(enableFirebase);

        reboot = new RebootButton();
        reboot.addActionListener(new DeviceRebootAction(this));
        this.add(reboot);

        takeScreenshotButton = new TakeScreenshotButton();
        takeScreenshotButton.addActionListener(new DeviceTakeScreenshotAction(this));
        this.add(takeScreenshotButton);

        uninstallApp = new UninstallAppButton();
        uninstallApp.addActionListener(new DeviceUninstallAppAction(this));
        this.add(uninstallApp);

        radio.setSelected(true);
        applyPanelState(devicePanelStateFactory.create(deviceInfo, icon));
        saveLogsButton.setVisible(true);
        logLocationButton.setVisible(true);
        eventTrackerButton.setVisible(true);
        screenMirrorButton.setVisible(true);
        screenRecordingButton.setVisible(true);
        wifiDebug.setVisible(true);
        enableFirebase.setVisible(true);
        reboot.setVisible(true);
        deviceTextPane.setText(deviceInfo.toDisplayText());
        deviceTextPane.setVisible(true);
        takeScreenshotButton.setVisible(true);
    }

    public void openExplorerToFolder(String folderPath) {
        try {
            deviceActionService.openFolder(folderPath);
        } catch (RuntimeException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public boolean isSelectedForInstall() {
        return radio.isSelected();
    }

    public void setRadioState(boolean radioState) {
        this.radioState = radioState;
    }

    public String getSerial() {
        return serial;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isAppInstalled() {
        return appIsInstalled;
    }

    public DeviceTarget toDeviceTarget() {
        return new DeviceTarget(serial, deviceName, isSelectedForInstall(), isAppInstalled());
    }

    private void applyPanelState(DevicePanelState panelState) {
        labelIcon.setIcon(panelState.getIcon());
        labelIcon.setText(panelState.getLabelText());
        labelIcon.setVisible(true);
        uninstallApp.setEnabled(panelState.isUninstallEnabled());
        uninstallApp.setVisible(true);
        enableFirebase.setEnabled(panelState.isEnableFirebaseEnabled());
        enableFirebase.setVisible(true);
        saveLogsButton.setEnabled(panelState.isSaveLogsEnabled());
        logLocationButton.setEnabled(panelState.isLogLocationEnabled());
        eventTrackerButton.setEnabled(panelState.isEventTrackerEnabled());
        screenMirrorButton.setEnabled(panelState.isScreenMirrorEnabled());
        screenRecordingButton.setEnabled(panelState.isScreenRecordingEnabled());
        takeScreenshotButton.setEnabled(panelState.isScreenshotEnabled());
        wifiDebug.setEnabled(panelState.isWifiDebugEnabled());
        reboot.setEnabled(panelState.isRebootEnabled());
        wifiDebug.setText(panelState.getWifiButtonText());
    }

    void saveLogs() {
        JFileChooser fileChooser = new JFileChooser(storagePaths.logsDir());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int response = fileChooser.showSaveDialog(parent);
        if (response == JFileChooser.APPROVE_OPTION) {
            file = new File(fileChooser.getSelectedFile().getAbsolutePath());
            logLocation = file.getAbsolutePath();
            String exportedLogsFolder = logExporter.exportDeviceLogs(new LogExportRequest(serial, file.getAbsolutePath())).getExportedFolder();
            eventTrackerButton.setEnabled(true);
            logLocationButton.setEnabled(true);
            openExplorerToFolder(exportedLogsFolder);
            parent.consoleView.appendText("SP logs from " + deviceName + " are saved to " + logLocation);
        }
    }

    void toggleWifiDebugging() {
        WifiDebugResult result = deviceOperations.toggleWifiDebugging(
                new WifiDebugRequest(
                        deviceInfo.getSerialNumber(),
                        deviceName,
                        deviceInfo.getIpAddress(),
                        deviceInfo.isWifiDebugSession(),
                        deviceInfo.hasWifiIp()
                )
        );

        if (result.isWifiConnectionRequired()) {
            JOptionPane.showMessageDialog(
                    parent,
                    result.getMessage(),
                    "Enable WiFi Debugging",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            boolean disablingWifiDebug = deviceInfo.isWifiDebugSession() && !result.isWifiEnabled();
            if (parent.isConsoleVisible) {
                parent.consoleView.appendText(result.getMessage());
                if (disablingWifiDebug) {
                    refreshDevicesMethod.run();
                } else {
                    reloadDeviceInfo();
                }
            } else {
                JOptionPane.showMessageDialog(parent, result.getMessage(), result.isWifiEnabled() ? "Enable WiFi Debugging" : "Disable WiFi Debugging.",
                        JOptionPane.INFORMATION_MESSAGE);
                if (disablingWifiDebug) {
                    refreshDevicesMethod.run();
                } else {
                    reloadDeviceInfo();
                }
            }
            wifiDebug.setText(result.getButtonText());
        }
    }

    void rebootDevice() {
        int response = JOptionPane.showConfirmDialog(parent, "Are you sure?", "Reboot the device",
                JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            DeviceMessageResult result = deviceOperations.rebootDevice(
                    new RebootDeviceRequest(deviceInfo.getSerialNumber(), deviceName)
            );
            if (parent.isConsoleVisible) {
                parent.consoleView.appendText(result.getMessage());
            }
        }
    }

    void takeScreenshot() {
        ScreenshotResult result = deviceOperations.captureScreenshot(
                new ScreenshotRequest(deviceInfo.getSerialNumber(), deviceName)
        );
        new ScreenshotFrame(deviceName, Device.this.totalDeviceCount);
        parent.consoleView.appendText(result.getMessage());
    }

    void enableFirebaseDebugging() {
        DeviceMessageResult result = deviceOperations.enableFirebaseDebugging(
                new FirebaseDebugRequest(deviceInfo.getSerialNumber(), deviceName, deviceInfo.getSafePathPackage())
        );
        if (parent.isConsoleVisible) {
            parent.consoleView.appendText(result.getMessage());
        } else {
            JOptionPane.showMessageDialog(parent,
                    result.getMessage(),
                    "Enable Firebase Debugging", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    void uninstallApp() {
        int response = JOptionPane.showConfirmDialog(parent, "Are you sure?", "Uninstall the app",
                JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            UninstallAppResult result = deviceOperations.uninstallApp(
                    new UninstallAppRequest(deviceInfo.getSerialNumber(), deviceName, deviceInfo.getSafePathPackage())
            );
            saveLogsButton.setEnabled(false);
            enableFirebase.setEnabled(false);
            labelIcon.setVisible(true);
            refreshDevicesMethod.run();
            if (parent.isConsoleVisible) {
                parent.consoleView.appendText(result.getMessage());
            } else {
                JOptionPane.showMessageDialog(parent, result.getMessage(), "Uninstall the app.",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    void openLogLocation() {
        openExplorerToFolder(logLocation);
    }

    void openEventTracker() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                liveEventTracker = new LiveEventTracker(serial, deviceInfo.getPid(), commandExecutor);
            }
        });
    }

    void startScreenMirror() {
        try {
            DeviceMessageResult result = deviceOperations.startScreenMirror(
                    new ScreenMirrorRequest(serial, deviceName)
            );
            parent.consoleView.appendText(result.getMessage());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void handleScreenRecording() {
        if (screenRecordingButton.getText().equals("Start Record")) {
            try {
                StartScreenRecordingResult result = deviceOperations.startScreenRecording(
                        new StartScreenRecordingRequest(serial, deviceName, recordingSession)
                );
                screenRecordingButton.setText(result.getButtonText());
                parent.consoleView.appendText(result.getMessage());
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(Device.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if(recordingSession.getRecordingInProgress().get()) {
            try {
                StopScreenRecordingResult result = deviceOperations.stopScreenRecording(
                        new StopScreenRecordingRequest(serial, deviceName, deviceInfo.getPid(), recordingSession)
                );
                screenRecordingButton.setText(result.getButtonText());
                if (result.isStopped()) {
                    recordingLocation = result.getRecordingLocation();
                    parent.consoleView.appendText(result.getMessage());
                    openExplorerToFolder(recordingLocation);
                } else {
                    JOptionPane.showMessageDialog(Device.this, result.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(Device.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            StopScreenRecordingResult result = new StopScreenRecordingResult(false, false, false, "No active recording!", "", "Start Record");
            JOptionPane.showMessageDialog(Device.this, result.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            screenRecordingButton.setText(result.getButtonText());
        }
    }

    private void reloadDeviceInfo() {
        deviceInfo = deviceInfoService.load(serial);
        appIsInstalled = deviceInfo.isAppInstalled();
        deviceTextPane.setText(deviceInfo.toDisplayText());
        applyPanelState(devicePanelStateFactory.create(deviceInfo, icon));
    }
}


