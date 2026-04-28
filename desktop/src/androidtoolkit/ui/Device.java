package androidtoolkit.ui;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceTarget;
import androidtoolkit.app.DeviceActionManager;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.LogExportManager;
import androidtoolkit.app.LogExportResponse;
import androidtoolkit.app.PermissionDefinition;
import androidtoolkit.app.PermissionDialogState;
import androidtoolkit.app.PermissionManager;
import androidtoolkit.app.PermissionUpdateResult;
import androidtoolkit.app.PermissionUpdateResponse;
import androidtoolkit.app.RecordingActionResponse;
import androidtoolkit.app.RecordingManager;
import androidtoolkit.app.ScreenshotCaptureResponse;
import androidtoolkit.app.ScreenshotManager;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;
import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.domain.RecordingSession;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.DeviceActionService;
import androidtoolkit.service.DeviceInfoService;
import androidtoolkit.service.StoragePaths;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Set;
import androidtoolkit.ui.components.*;

public class Device extends JPanel {

    private final StoragePaths storagePaths;
    private final DeviceInfoService deviceInfoService;
    private final CommandExecutor commandExecutor;
    private final DeviceActionService deviceActionService;
    private final DeviceActionManager deviceActionManager;
    private final LogExportManager logExportManager;
    private final DevicePanelStateFactory devicePanelStateFactory;
    private final Icons icon;
    private final int screenshotFrameCount;
    private final String serial;
    private final String deviceName;
    private final MyFrame parent;
    private final Runnable refreshDevicesMethod;
    private final SaveSpLogsButton saveLogsButton;
    private final LogLocationButton logLocationButton;
    private final WifiDebugButton wifiDebug;
    private final EnableFirebaseButton enableFirebase;
    private final RebootButton reboot;
    private final TakeScreenshotButton takeScreenshotButton;
    private final UninstallAppButton uninstallApp;
    private final DeviceSelectionRadioButton radio;
    private final LogoIconLabels labelIcon;
    private final DeviceTextPanes deviceTextPane;
    private final EventTrackerButton eventTrackerButton;
    private final ScreenMirrorButton screenMirrorButton;
    private final ScreenRecordingButton screenRecordingButton;
    private final PermissionsButton permissionsButton;
    private final RecordingSession recordingSession = new RecordingSession();
    private final PermissionManager permissionManager;
    private final RecordingManager recordingManager;
    private final ScreenshotManager screenshotManager;
    private DeviceInfo deviceInfo;
    private boolean appIsInstalled;
    private String logLocation;

    public Device(MyFrame parent, ConnectedDevice connectedDevice, int totalDeviceCount, Runnable refreshDevicesMethod, AppServices appServices) {

        this.setBounds((connectedDevice.getIndex()+1)*210, 0, 210, 345);
        this.setLayout(null);
        this.screenshotFrameCount = totalDeviceCount;
        this.storagePaths = appServices.storagePaths();
        this.deviceInfoService = appServices.deviceInfoService();
        this.commandExecutor = appServices.commandExecutor();
        this.logLocation = storagePaths.logsDir().getPath();
        this.icon = new Icons();
        this.deviceActionService = appServices.deviceActionService();
        this.deviceActionManager = appServices.deviceActionManager();
        this.permissionManager = appServices.permissionManager();
        this.recordingManager = appServices.recordingManager();
        this.screenshotManager = appServices.screenshotManager();
        this.logExportManager = appServices.logExportManager();
        this.devicePanelStateFactory = new DevicePanelStateFactory();
        this.serial = connectedDevice.getSerial();
        this.deviceName = connectedDevice.getDeviceName();
        this.deviceInfo = connectedDevice.getDeviceInfo();
        this.appIsInstalled = deviceInfo.isAppInstalled();
        this.parent = parent;
        this.refreshDevicesMethod = refreshDevicesMethod;

        this.radio = new DeviceSelectionRadioButton(deviceName);
        this.labelIcon = new LogoIconLabels(icon.notInstalled);
        this.deviceTextPane = new DeviceTextPanes();
        this.eventTrackerButton = new EventTrackerButton();
        this.saveLogsButton = new SaveSpLogsButton();
        this.logLocationButton = new LogLocationButton();
        this.screenMirrorButton = new ScreenMirrorButton();
        this.screenRecordingButton = new ScreenRecordingButton();
        this.permissionsButton = new PermissionsButton();
        this.wifiDebug = new WifiDebugButton();
        this.enableFirebase = new EnableFirebaseButton();
        this.reboot = new RebootButton();
        this.takeScreenshotButton = new TakeScreenshotButton();
        this.uninstallApp = new UninstallAppButton();

        setIconAndButtons(totalDeviceCount);
        this.setVisible(false);
    }

    private void setIconAndButtons(int totalDeviceCount) {
        radio.setVisible(true);
        this.add(radio);

        labelIcon.setVisible(true);
        this.add(labelIcon);

        deviceTextPane.setText(deviceInfo.toDisplayText());
        deviceTextPane.setVisible(true);
        this.add(deviceTextPane);

        eventTrackerButton.addActionListener(new DeviceEventTrackerAction(this));
        this.add(eventTrackerButton);

        saveLogsButton.addActionListener(new DeviceSaveLogsAction(this));
        this.add(saveLogsButton);

        logLocationButton.addActionListener(new DeviceLogLocationAction(this));
        this.add(logLocationButton);

        screenMirrorButton.addActionListener(new DeviceScreenMirrorAction(this));
        this.add(screenMirrorButton);

        screenRecordingButton.addActionListener(new DeviceScreenRecordingAction(this));
        this.add(screenRecordingButton);

        permissionsButton.addActionListener(new DevicePermissionsAction(this));
        this.add(permissionsButton);

        wifiDebug.addActionListener(new DeviceToggleWifiDebugAction(this));
        this.add(wifiDebug);

        enableFirebase.addActionListener(new DeviceEnableFirebaseAction(this));
        this.add(enableFirebase);

        reboot.addActionListener(new DeviceRebootAction(this));
        this.add(reboot);

        takeScreenshotButton.addActionListener(new DeviceTakeScreenshotAction(this));
        this.add(takeScreenshotButton);

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
        permissionsButton.setVisible(true);
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
        permissionsButton.setEnabled(panelState.isPermissionsEnabled());
        wifiDebug.setText(panelState.getWifiButtonText());
    }

    void openPermissionsDialog() {
        PermissionDialogState dialogState = permissionManager.loadDialogState(serial, deviceInfo.getSafePathPackage());
        if (!dialogState.hasDefinitions()) {
            JOptionPane.showMessageDialog(parent, "No predefined permissions are available for this package.", "Permissions",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        PermissionsDialog dialog = new PermissionsDialog(
                parent,
                deviceName,
                dialogState.getPackageName(),
                dialogState.getDefinitions(),
                Set.copyOf(dialogState.getActivePermissionIds()),
                Set.copyOf(dialogState.getUnavailablePermissionIds())
        );
        dialog.setEnableAction(event -> runPermissionUpdate(dialog, dialogState, true));
        dialog.setDisableAction(event -> runPermissionUpdate(dialog, dialogState, false));
        dialog.setVisible(true);
    }

    private void runPermissionUpdate(PermissionsDialog dialog, PermissionDialogState dialogState, boolean enable) {
        java.util.List<PermissionDefinition> selectedPermissions = dialog.selectedDefinitions();
        dialog.setActionsEnabled(false);
        SwingWorker<PermissionUpdateResponse, Void> worker = new SwingWorker<PermissionUpdateResponse, Void>() {
            @Override
            protected PermissionUpdateResponse doInBackground() {
                if (enable) {
                    return permissionManager.enablePermissions(serial, dialogState.getPackageName(), selectedPermissions);
                }
                return permissionManager.disablePermissions(serial, dialogState.getPackageName(), selectedPermissions);
            }

            @Override
            protected void done() {
                dialog.setActionsEnabled(true);
                try {
                    PermissionUpdateResponse response = get();
                    PermissionUpdateResult result = response.getUpdateResult();
                    PermissionDialogState refreshedState = response.getDialogState();
                    dialog.updateStatuses(
                            Set.copyOf(refreshedState.getActivePermissionIds()),
                            Set.copyOf(refreshedState.getUnavailablePermissionIds())
                    );
                    parent.appendConsoleText(result.toDisplayMessage(deviceName));
                    dialog.showResult(result, deviceName);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Permissions", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    void saveLogs() {
        JFileChooser fileChooser = new JFileChooser(storagePaths.logsDir());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int response = fileChooser.showSaveDialog(parent);
        if (response == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = new File(fileChooser.getSelectedFile().getAbsolutePath());
            LogExportResponse exportResponse = logExportManager.exportDeviceLogs(serial, deviceName, selectedFolder.getAbsolutePath());
            logLocation = exportResponse.getSelectedFolder();
            eventTrackerButton.setEnabled(true);
            logLocationButton.setEnabled(true);
            openExplorerToFolder(exportResponse.getExportedLogsFolder());
            parent.appendConsoleText(exportResponse.getMessage());
        }
    }

    void toggleWifiDebugging() {
        WifiDebugResult result = deviceActionManager.toggleWifiDebugging(
                deviceInfo.getSerialNumber(),
                deviceName,
                deviceInfo.getIpAddress(),
                deviceInfo.isWifiDebugSession(),
                deviceInfo.hasWifiIp()
        );

        if (result.isWifiConnectionRequired()) {
            JOptionPane.showMessageDialog(
                    parent,
                    result.getMessage(),
                    "Enable WiFi Debugging",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            boolean disablingWifiDebug = deviceInfo.isWifiDebugSession() && !result.isWifiEnabled();
            if (parent.isConsoleVisible()) {
                parent.appendConsoleText(result.getMessage());
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
            DeviceMessageResult result = deviceActionManager.rebootDevice(deviceInfo.getSerialNumber(), deviceName);
            if (parent.isConsoleVisible()) {
                parent.appendConsoleText(result.getMessage());
            }
        }
    }

    void takeScreenshot() {
        ScreenshotCaptureResponse result = screenshotManager.captureScreenshot(deviceInfo.getSerialNumber(), deviceName);
        new ScreenshotFrame(deviceName, screenshotFrameCount, storagePaths);
        parent.appendConsoleText(result.getMessage());
    }

    void enableFirebaseDebugging() {
        DeviceMessageResult result = deviceActionManager.enableFirebaseDebugging(
                deviceInfo.getSerialNumber(),
                deviceName,
                deviceInfo.getSafePathPackage()
        );
        if (parent.isConsoleVisible()) {
            parent.appendConsoleText(result.getMessage());
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
            UninstallAppResult result = deviceActionManager.uninstallApp(
                    deviceInfo.getSerialNumber(),
                    deviceName,
                    deviceInfo.getSafePathPackage()
            );
            saveLogsButton.setEnabled(false);
            enableFirebase.setEnabled(false);
            labelIcon.setVisible(true);
            refreshDevicesMethod.run();
            if (parent.isConsoleVisible()) {
                parent.appendConsoleText(result.getMessage());
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
                new LiveEventTracker(serial, deviceInfo.getPid(), commandExecutor);
            }
        });
    }

    void startScreenMirror() {
        try {
            DeviceMessageResult result = deviceActionManager.startScreenMirror(serial, deviceName);
            parent.appendConsoleText(result.getMessage());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void handleScreenRecording() {
        if (screenRecordingButton.getText().equals("Start Record")) {
            try {
                RecordingActionResponse result = recordingManager.startRecording(serial, deviceName, recordingSession);
                screenRecordingButton.setText(result.getButtonText());
                parent.appendConsoleText(result.getMessage());
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(Device.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (recordingSession.isActive()) {
            try {
                RecordingActionResponse result = recordingManager.stopRecording(serial, deviceName, deviceInfo.getPid(), recordingSession);
                screenRecordingButton.setText(result.getButtonText());
                if (result.isSuccess()) {
                    parent.appendConsoleText(result.getMessage());
                    openExplorerToFolder(result.getRecordingLocation());
                } else {
                    JOptionPane.showMessageDialog(Device.this, result.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(Device.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            RecordingActionResponse result = recordingManager.noActiveRecording();
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


