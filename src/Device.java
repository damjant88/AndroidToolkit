import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import Buttons.*;

public class Device extends JPanel {

    Util utility;
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
    String logLocation = "C:/AdbToolkit/Logs/";
    String recordingLocation = "C:/AdbToolkit/Screen_Recordings/";
    EventTrackerButtons eventTrackerButton;
    ScreenMirrorButtons screenMirrorButton;
    ScreenRecordingButtons screenRecordingButton;
    private String recordingFileName;
    private AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    private Process recordingProcess;
    ConsoleView consoleView;
    LiveEventTracker liveEventTracker;

    public Device(MyFrame parent, int index, Runnable refreshDevicesMethod) {

        this.setBounds((index+1)*210, 0, 210, 310);
        this.setLayout(null);
        icon = new Icons();
        utility = new Util();
        serialNumberList = utility.getConnectedDevices();
        numberOfDevices = serialNumberList.size();
        serial = serialNumberList.get(index);
        deviceInfo = new DeviceInfo(serial);
        appIsInstalled = deviceInfo.appIsInstalled;
        setIconAndButtons(index);
        this.setVisible(false);
        this.parent = parent;
        this.refreshDevicesMethod = refreshDevicesMethod;
        consoleView = new ConsoleView(parent);
    }

    private void setIconAndButtons(int i) {
        deviceName = "Device"+(i+1);
        radio = new RadioButtons(deviceName);
        radio.setVisible(true);
        this.add(radio);

        labelIcon = new LogoIconLabels(icon.notInstalled);
        labelIcon.setVisible(true);
        this.add(labelIcon);

        deviceTextPane = new DeviceTextPanes();
        deviceTextPane.setText(deviceInfo.serialNo + "\n" + deviceInfo.manufacturer + "\n"
                + deviceInfo.model + "\n" + "Android "
                + deviceInfo.OSVersion + "\n" + deviceInfo.ip);
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

        switch (deviceInfo.safePathPackage) {
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
            case "com.smithmicro.att.securefamily":
            case "com.att.securefamilycompanion": {
                labelIcon.setIcon(icon.logo_att);
                labelIcon.setText("SecureFamily");
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
            case "com.smithmicro.orangespain.test": {
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
        if (deviceInfo.serialNo.endsWith(":5555")) {
            wifiDebug.setText("Disable WiFi");
        }
        wifiDebug.setVisible(true);
        enableFirebase.setVisible(true);
        reboot.setVisible(true);
        deviceTextPane.setText(deviceInfo.serialNo + "\n" + deviceInfo.manufacturer + "\n"
                + deviceInfo.model + "\n" + "Android "
                + deviceInfo.OSVersion + "\n" + deviceInfo.ip);
        deviceTextPane.setVisible(true);
        takeScreenshotButton.setVisible(true);
    }

    class SaveSPLogsButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser("C:/AdbToolkit/Logs");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int response = fileChooser.showSaveDialog(parent);
            if (response == JFileChooser.APPROVE_OPTION) {
                file = new File(fileChooser.getSelectedFile().getAbsolutePath());
                logLocation = file.getAbsolutePath();
                String appFlavour = utility.getSafePathPackage(serial);
                utility.saveLogs(serial, appFlavour, file.getAbsolutePath());
                eventTrackerButton.setEnabled(true);
                logLocationButton.setEnabled(true);
                openExplorerToFolder(logLocation + "/logs/");
                parent.consoleView.appendText("SP logs from " + deviceName + " are saved to " + logLocation);
            }
        }
    }

    class WifiDebugListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (deviceInfo.wifiIP.isEmpty()) {
                JOptionPane.showMessageDialog(
                        parent,
                        "Connect the device " + deviceName + " to WiFi and click on 'Display Connected Devices' button to refresh IP! ",
                        "Enable WiFi Debugging",
                        JOptionPane.INFORMATION_MESSAGE);

            } else if (!deviceInfo.serialNo.endsWith(":5555")) {
                utility.startWifiDebugging(deviceInfo.serialNo, deviceInfo.ip);
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
                utility.stopWifiDebugging(deviceInfo.serialNo, deviceInfo.ip);
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
                utility.reboot(deviceInfo.serialNo);
            }
            if (parent.isConsoleVisible) {
                parent.consoleView.appendText(deviceName + " is restarted!");
            }
        }
    }

    class TakeScreenshotButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String output = utility.takeScreenshot(deviceInfo.serialNo, "sdcard/", "screenshot.png");
            File device = new File("C:/AdbToolkit/Screenshots/" + deviceName);
            if (!device.exists()) {
                device.mkdirs();
            }
            utility.pullFile(deviceInfo.serialNo, output, "C:/AdbToolkit/Screenshots/" + deviceName);
            ScreenshotFrame screenshotFrame = new ScreenshotFrame(deviceName, numberOfDevices);
            parent.consoleView.appendText("Screenhot is captured on " + deviceName);
        }
    }

    class EnableFirebaseListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            utility.enableAnalyticsDebug(deviceInfo.serialNo, deviceInfo.safePathPackage);
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
                utility.uninstallApp(deviceInfo.serialNo, deviceInfo.safePathPackage);
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
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            try {
                Desktop.getDesktop().open(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Folder does not exist or is not a directory: " + folderPath);
        }
    }

    class EventTrackerListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    liveEventTracker = new LiveEventTracker(serial, deviceInfo.pid);
                    System.out.println("Tracker Opened!");
                }
            });
        }
    }

    private void startScreenMirror(String serial) {
        // Check if scrcpy executable exists in PATH

        String[] pathDirectories = System.getenv("PATH").split(File.pathSeparator);
        File scrcpyExecutable = null;
        boolean found = false;
        for (String directory : pathDirectories) {
            scrcpyExecutable = new File(directory, "scrcpy.exe");
            if (scrcpyExecutable.exists()) {
                found = true;
                break;
            }
        }
        if (!found) {
            JOptionPane.showMessageDialog(parent, "scrcpy executable not found in System variables Path!", "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Run scrcpy for the specific device
        File finalScrcpyExecutable = scrcpyExecutable;
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    ProcessBuilder pb = new ProcessBuilder(finalScrcpyExecutable.getAbsolutePath(), "-s", serial);
                    pb.redirectErrorStream(true);
                    pb.start().waitFor();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(Device.this, "Failed to start scrcpy for device with serial: " + serial, "Error", JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }
        };
        worker.execute();
    }

    private void startScreenRecording(String serial) {
        // Check if Adb Toolkit directory exists, if not, create it
        File toolkitDir = new File("C:/AdbToolkit/Screen_Recordings");
        if (!toolkitDir.exists()) {
            toolkitDir.mkdirs();
        }

        // Generate recording file name
        recordingFileName = "screen_record_" + System.currentTimeMillis() + ".mp4";
        startScreenMirror(serial);

        // Start screen recording on the device
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                recordingInProgress.set(true);
                try {
                    ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "shell", "screenrecord", "/sdcard/" + recordingFileName);
                    pb.redirectErrorStream(true);
                    recordingProcess = pb.start();
                    recordingProcess.waitFor();
                } catch (InterruptedException ex) {
                    System.out.println("Screen recording interrupted.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(Device.this, "Failed to start screen recording for device with serial: " + serial, "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    recordingInProgress.set(false);
                }
                return null;
            }
        };
        worker.execute();
        screenRecordingButton.setText("Stop Record");
    }

    private void stopScreenRecording(String serial, String recordingFileName, String deviceName) throws InterruptedException {
        if (recordingProcess != null) {
            recordingProcess.destroy();
            try {
                // Wait for the process to terminate
                LocalDate currentDate = LocalDate.now();
                String dateString = currentDate.toString();
                recordingLocation = "C:/AdbToolkit/Screen_Recordings/" + deviceName + "/" + dateString ;
                File device = new File(recordingLocation);
                if (!device.exists()) {
                    device.mkdirs();
                }
                Thread.sleep(300);
                utility.runCommand("adb -s " + serial + " pull " + "/sdcard/" + recordingFileName + " " + recordingLocation);
                recordingProcess = null;
                recordingInProgress.set(false);
                screenRecordingButton.setText("Start Record");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ScreenMirrorButtonsListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            startScreenMirror(serial);
            parent.consoleView.appendText("Screen mirror is started on " + deviceName);
        }
    }

    class ScreenRecordingButtonsListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (screenRecordingButton.getText().equals("Start Record")) {
                startScreenRecording(serial);
                parent.consoleView.appendText("Screen recording is started on " + deviceName);
            } else if(recordingInProgress.get()) {
                try {
                    stopScreenRecording(serial, recordingFileName, deviceName);
                    utility.runCommand("adb -s " + serial + " shell rm " + "/sdcard/" + recordingFileName);
                    String appFlavour = utility.getSafePathPackage(serial);
                    utility.saveLogs(serial, appFlavour, recordingLocation);
                    parent.consoleView.appendText("Screen recording is stopped on " + deviceName + "\n" + "Screen recording saved to:\n" + recordingLocation);
                    openExplorerToFolder(recordingLocation);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                JOptionPane.showMessageDialog(Device.this, "No active recording!", "Error", JOptionPane.ERROR_MESSAGE);
                screenRecordingButton.setText("Start Record");
            }
        }
    }
}
