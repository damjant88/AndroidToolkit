import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import Buttons.*;

public class Device extends JPanel {

    private static final StoragePaths STORAGE_PATHS = new StoragePaths();
    Util utility;
    DeviceActionService deviceActionService;
    ScreenRecordingService screenRecordingService;
    DeviceInfoService deviceInfoService;
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
    int numberOfDevices;
    String serial;
    Boolean appIsInstalled;
    String deviceName;
    MyFrame parent;
    ArrayList<String> serialNumberList;
    Runnable refreshDevicesMethod;
    String logLocation = STORAGE_PATHS.logsDir().getPath();
    String recordingLocation = STORAGE_PATHS.screenRecordingsDir().getPath();
    EventTrackerButtons eventTrackerButton;
    ScreenMirrorButtons screenMirrorButton;
    ScreenRecordingButtons screenRecordingButton;
    private final RecordingSession recordingSession = new RecordingSession();
    ConsoleView consoleView;
    LiveEventTracker liveEventTracker;

    public Device(MyFrame parent, int index, Runnable refreshDevicesMethod) {

        this.setBounds((index+1)*210, 0, 210, 310);
        this.setLayout(null);
        icon = new Icons();
        utility = new Util();
        deviceActionService = new DeviceActionService(utility, STORAGE_PATHS);
        screenRecordingService = new ScreenRecordingService(utility, STORAGE_PATHS);
        deviceInfoService = new DeviceInfoService(utility);
        serialNumberList = utility.getConnectedDevices();
        numberOfDevices = serialNumberList.size();
        serial = serialNumberList.get(index);
        System.out.println(serial);
        deviceInfo = deviceInfoService.load(serial);
        appIsInstalled = deviceInfo.isAppInstalled();
        System.out.println(appIsInstalled);
        setIconAndButtons(index);
        this.setVisible(false);
        this.parent = parent;
        this.refreshDevicesMethod = refreshDevicesMethod;
        consoleView = new ConsoleView(parent);
    }

    private void setIconAndButtons(int i) {
        deviceName = "Device"+(i+1);
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

        switch (deviceInfo.getSafePathPackage()) {
            case "com.smithmicro.tmobile.familymode.test":
            case "com.tmobile.familycontrols": {
                labelIcon.setIcon(icon.logo_tmo);
                labelIcon.setText("FamilyMode");
                labelIcon.setVisible(true);
                uninstallApp.setEnabled(true);
                uninstallApp.setVisible(true);
                enableFirebase.setEnabled(true);
                enableFirebase.setVisible(true);
                saveLogsButton.setEnabled(true);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(true);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
            case "com.smithmicro.safepath.dish.test":
            case "com.smithmicro.safepath.dish.kid.test": {
                labelIcon.setIcon(icon.logo_dish);
                labelIcon.setText("Dish");
                labelIcon.setVisible(true);
                uninstallApp.setEnabled(true);
                uninstallApp.setVisible(true);
                enableFirebase.setEnabled(true);
                enableFirebase.setVisible(true);
                saveLogsButton.setEnabled(true);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(true);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
            case "com.smithmicro.safepath.family":
            case "com.smithmicro.safepath.family.child": {
                labelIcon.setIcon(icon.logo_product);
                labelIcon.setText("SPFamily");
                labelIcon.setVisible(true);
                uninstallApp.setEnabled(true);
                uninstallApp.setVisible(true);
                enableFirebase.setEnabled(true);
                enableFirebase.setVisible(true);
                saveLogsButton.setEnabled(true);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(true);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
            case "com.smithmicro.att.securefamily": {
                labelIcon.setIcon(icon.logo_att);
                labelIcon.setText("SF IAP");
                labelIcon.setVisible(true);
                uninstallApp.setEnabled(true);
                uninstallApp.setVisible(true);
                enableFirebase.setEnabled(true);
                enableFirebase.setVisible(true);
                saveLogsButton.setEnabled(true);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(true);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
            case "com.wavemarket.waplauncher": {
                labelIcon.setIcon(icon.logo_att);
                labelIcon.setText("SF EAP");
                labelIcon.setVisible(true);
                uninstallApp.setEnabled(true);
                uninstallApp.setVisible(true);
                enableFirebase.setEnabled(true);
                enableFirebase.setVisible(true);
                saveLogsButton.setEnabled(true);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(true);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
            case "com.att.securefamilycompanion": {
                labelIcon.setIcon(icon.logo_att);
                labelIcon.setText("SF Companion");
                labelIcon.setVisible(true);
                uninstallApp.setEnabled(true);
                uninstallApp.setVisible(true);
                enableFirebase.setEnabled(true);
                enableFirebase.setVisible(true);
                saveLogsButton.setEnabled(true);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(true);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
            case "com.smithmicro.sprint.safeandfound.test":
            case "com.sprint.safefound": {
                labelIcon.setIcon(icon.logo_sprint);
                labelIcon.setText("Safe&Found");
                labelIcon.setVisible(true);
                uninstallApp.setEnabled(true);
                uninstallApp.setVisible(true);
                enableFirebase.setEnabled(true);
                enableFirebase.setVisible(true);
                saveLogsButton.setEnabled(true);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(true);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
            case "com.smithmicro.orangespain.test":
            case "com.orange.es.TuYo": {
                labelIcon.setIcon(icon.logo_orange);
                labelIcon.setText("TuYo");
                labelIcon.setVisible(true);
                uninstallApp.setEnabled(true);
                uninstallApp.setVisible(true);
                enableFirebase.setEnabled(true);
                enableFirebase.setVisible(true);
                saveLogsButton.setEnabled(true);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(true);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
            case "": {
                labelIcon.setIcon(icon.notInstalled);
                labelIcon.setText("Not Installed");
                labelIcon.setVisible(true);
                uninstallApp.setVisible(true);
                enableFirebase.setVisible(true);
                enableFirebase.setEnabled(false);
                saveLogsButton.setEnabled(false);
                logLocationButton.setEnabled(false);
                eventTrackerButton.setEnabled(false);
                screenMirrorButton.setEnabled(true);
                screenRecordingButton.setEnabled(true);
                takeScreenshotButton.setEnabled(true);
                wifiDebug.setEnabled(true);
                reboot.setEnabled(true);
                break;
            }
        }
        saveLogsButton.setVisible(true);
        logLocationButton.setVisible(true);
        eventTrackerButton.setVisible(true);
        screenMirrorButton.setVisible(true);
        screenRecordingButton.setVisible(true);
        wifiDebug.setText("WiFi Debug");
        if (deviceInfo.isWifiDebugSession()) {
            wifiDebug.setText("Disable WiFi");
        }
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
            JFileChooser fileChooser = new JFileChooser(STORAGE_PATHS.logsDir());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int response = fileChooser.showSaveDialog(parent);
            if (response == JFileChooser.APPROVE_OPTION) {
                file = new File(fileChooser.getSelectedFile().getAbsolutePath());
                logLocation = file.getAbsolutePath();
                String exportedLogsFolder = deviceActionService.saveLogs(serial, file.getAbsolutePath());
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
            new ScreenshotFrame(deviceName, numberOfDevices);
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

    public static void openExplorerToFolder(String folderPath) {
        try {
            new DeviceActionService(new Util(), STORAGE_PATHS).openFolder(folderPath);
        } catch (RuntimeException ex) {
            System.err.println(ex.getMessage());
        }
    }

    class EventTrackerListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    liveEventTracker = new LiveEventTracker(serial, deviceInfo.getPid());
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
