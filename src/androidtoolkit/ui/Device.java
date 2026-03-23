package androidtoolkit.ui;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceTarget;
import androidtoolkit.app.LogExportRequest;
import androidtoolkit.app.LogExporter;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import Buttons.*;

public class Device extends JPanel {

    private final StoragePaths storagePaths;
    private final DeviceGateway deviceGateway;
    private final CommandExecutor commandExecutor;
    DeviceActionService deviceActionService;
    ScreenRecordingService screenRecordingService;
    LogExporter logExporter;
    DevicePanelStateFactory devicePanelStateFactory;
    File file = null;
    SaveSPLogsButtons saveLogsButton;
    LogLocationButtons logLocationButton;
    WifiDebugButtons wifiDebug;
    EnableFirebaseButtons enableFirebase;
    RebootButtons reboot;
    TakeScreenshotButtons takeScreenshotButton;
    UninstallAppButtons uninstallApp;
    RadioButtons radio;
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
    EventTrackerButtons eventTrackerButton;
    ScreenMirrorButtons screenMirrorButton;
    ScreenRecordingButtons screenRecordingButton;
    private final RecordingSession recordingSession = new RecordingSession();
    ConsoleView consoleView;
    LiveEventTracker liveEventTracker;

    public Device(MyFrame parent, ConnectedDevice connectedDevice, int totalDeviceCount, Runnable refreshDevicesMethod, AppServices appServices) {

        this.setBounds((connectedDevice.getIndex()+1)*210, 0, 210, 310);
        this.setLayout(null);
        this.totalDeviceCount = totalDeviceCount;
        this.storagePaths = appServices.storagePaths();
        this.deviceGateway = appServices.deviceGateway();
        this.commandExecutor = appServices.commandExecutor();
        this.logLocation = storagePaths.logsDir().getPath();
        this.recordingLocation = storagePaths.screenRecordingsDir().getPath();
        icon = new Icons();
        deviceActionService = appServices.deviceActionService();
        screenRecordingService = appServices.screenRecordingService();
        logExporter = appServices.logExporter();
        devicePanelStateFactory = new DevicePanelStateFactory();
        serial = connectedDevice.getSerial();
        System.out.println(serial);
        deviceName = connectedDevice.getDeviceName();
        deviceInfo = connectedDevice.getDeviceInfo();
        appIsInstalled = deviceInfo.isAppInstalled();
        System.out.println(appIsInstalled);
        setIconAndButtons(totalDeviceCount);
        this.setVisible(false);
        this.parent = parent;
        this.refreshDevicesMethod = refreshDevicesMethod;
        consoleView = new ConsoleView(parent, commandExecutor);
    }

    private void setIconAndButtons(int totalDeviceCount) {
        System.out.println(deviceName);
        radio = new RadioButtons(deviceName);
        radio.setVisible(true);
        this.add(radio);

        labelIcon = new LogoIconLabels(icon.notInstalled);
        labelIcon.setVisible(true);
        this.add(labelIcon);

        deviceTextPane = new DeviceTextPanes();
        deviceTextPane.setText(deviceInfo.toDisplayText());
        deviceTextPane.setVisible(true);
        this.add(deviceTextPane);

        eventTrackerButton = new EventTrackerButtons();
        eventTrackerButton.addActionListener(new EventTrackerListener());
        this.add(eventTrackerButton);

        saveLogsButton = new SaveSPLogsButtons();
        saveLogsButton.addActionListener(new SaveSPLogsButtonListener());
        this.add(saveLogsButton);

        logLocationButton = new LogLocationButtons();
        logLocationButton.addActionListener(new LogLocationButtonsListener());
        this.add(logLocationButton);

        screenMirrorButton = new ScreenMirrorButtons();
        screenMirrorButton.addActionListener(new ScreenMirrorButtonsListener());
        this.add(screenMirrorButton);

        screenRecordingButton = new ScreenRecordingButtons();
        screenRecordingButton.addActionListener(new ScreenRecordingButtonsListener());
        this.add(screenRecordingButton);

        wifiDebug = new WifiDebugButtons();
        wifiDebug.addActionListener(new WifiDebugListener());
        this.add(wifiDebug);

        enableFirebase = new EnableFirebaseButtons();
        enableFirebase.addActionListener(new EnableFirebaseListener());
        this.add(enableFirebase);

        reboot = new RebootButtons();
        reboot.addActionListener(new RebootListener());
        this.add(reboot);

        takeScreenshotButton = new TakeScreenshotButtons();
        takeScreenshotButton.addActionListener(new TakeScreenshotButtonListener());
        this.add(takeScreenshotButton);

        uninstallApp = new UninstallAppButtons();
        uninstallApp.addActionListener(new UninstallAppListener());
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

    class SaveSPLogsButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
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
    }

    class WifiDebugListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!deviceInfo.hasWifiIp()) {
                JOptionPane.showMessageDialog(
                        parent,
                        "Connect the device " + deviceName + " to WiFi and click on 'Display Connected Devices' button to refresh IP! ",
                        "Enable WiFi Debugging",
                        JOptionPane.INFORMATION_MESSAGE);

            } else if (!deviceInfo.isWifiDebugSession()) {
                deviceActionService.enableWifiDebugging(deviceInfo.getSerialNumber(), deviceInfo.getIpAddress());
                if (parent.isConsoleVisible) {
                    parent.consoleView.appendText("WiFi debugging is enabled on " + deviceName + "!\n" +
                            "If prompted on the device, allow wireless debugging on specific wifi network.\n" +
                            "You may disconnect USB cable from this device.");
                } else {
                    JOptionPane.showMessageDialog(
                            parent,
                            "Debugging over WiFi is enabled on " + deviceName + "!\n" +
                                    "If prompted on the device, allow wireless debugging on specific wifi network.\n" +
                                    "You may disconnect USB cable from this device.",
                            "Enable WiFi Debugging",
                            JOptionPane.INFORMATION_MESSAGE);
                    wifiDebug.setText("Disable WiFi");
                }
            } else {
                deviceActionService.disableWifiDebugging(deviceInfo.getSerialNumber(), deviceInfo.getIpAddress());
                if (parent.isConsoleVisible) {
                    parent.consoleView.appendText("WiFi debugging is disabled on " + deviceName);
                } else {
                    JOptionPane.showMessageDialog(parent, "Debugging over WiFi is disabled on " + deviceName + "!", "Disable WiFi Debugging.",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    class RebootListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int response = JOptionPane.showConfirmDialog(parent, "Are you sure?", "Reboot the device",
                    JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                deviceActionService.reboot(deviceInfo.getSerialNumber());
            }
            if (parent.isConsoleVisible) {
                parent.consoleView.appendText(deviceName + " is restarted!");
            }
        }
    }

    class TakeScreenshotButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            deviceActionService.captureScreenshot(deviceInfo.getSerialNumber(), deviceName);
            new ScreenshotFrame(deviceName, Device.this.totalDeviceCount);
            parent.consoleView.appendText("Screenhot is captured on " + deviceName);
        }
    }

    class EnableFirebaseListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            deviceActionService.enableFirebaseDebugging(deviceInfo.getSerialNumber(), deviceInfo.getSafePathPackage());
            if (parent.isConsoleVisible) {
                parent.consoleView.appendText("Firebase Debugging enabled on " + deviceName + "!" + "\n"
                        + "Make sure 'Logging Analytics Events' toggle button is also enabled in Debug menu.");
            } else {
                JOptionPane.showMessageDialog(parent,
                        "Firebase Debugging enabled on " + deviceName + "!" + "\n"
                                + "Make sure 'Logging Analytics Events' toggle button is also enabled in Debug menu.",
                        "Enable Firebase Debugging", JOptionPane.INFORMATION_MESSAGE);
            }

        }
    }

    class UninstallAppListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int response = JOptionPane.showConfirmDialog(parent, "Are you sure?", "Uninstall the app",
                    JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                deviceActionService.uninstallApp(deviceInfo.getSerialNumber(), deviceInfo.getSafePathPackage());
                saveLogsButton.setEnabled(false);
                enableFirebase.setEnabled(false);
                labelIcon.setVisible(true);
                refreshDevicesMethod.run();
                if (parent.isConsoleVisible) {
                    parent.consoleView.appendText("App is uninstalled from " + deviceName + "!");
                    System.out.println(parent.isConsoleVisible);
                } else {
                    JOptionPane.showMessageDialog(parent, "App is uninstalled from " + deviceName + "!", "Uninstall the app.",
                            JOptionPane.INFORMATION_MESSAGE);
                    System.out.println(parent.isConsoleVisible);
                }
            }
        }
    }

    class LogLocationButtonsListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            openExplorerToFolder(logLocation);
        }
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

    class EventTrackerListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    liveEventTracker = new LiveEventTracker(serial, deviceInfo.getPid(), commandExecutor);
                    System.out.println("Tracker Opened!");
                }
            });
        }
    }

    class ScreenMirrorButtonsListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                screenRecordingService.startScreenMirrorAsync(serial);
                parent.consoleView.appendText("Screen mirror is started on " + deviceName);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(parent, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class ScreenRecordingButtonsListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (screenRecordingButton.getText().equals("Start Record")) {
                try {
                    screenRecordingService.startScreenRecording(serial, recordingSession);
                    screenRecordingButton.setText("Stop Record");
                    parent.consoleView.appendText("Screen recording is started on " + deviceName + ".");
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(Device.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if(recordingSession.getRecordingInProgress().get()) {
                try {
                    recordingLocation = screenRecordingService.stopScreenRecording(serial, deviceName, recordingSession);
                    screenRecordingButton.setText("Start Record");
                    parent.consoleView.appendText("Screen recording is stopped on " + deviceName + "." + "\n" + "Screen recording saved to:\n" + recordingLocation);
                    openExplorerToFolder(recordingLocation);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(Device.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(Device.this, "No active recording!", "Error", JOptionPane.ERROR_MESSAGE);
                screenRecordingButton.setText("Start Record");
            }
        }
    }
}
