package androidtoolkit.service;

import java.io.File;
import java.io.IOException;

public class DeviceActionService {

    private final AdbDeviceService adbDeviceService;
    private final StoragePaths storagePaths;

    public DeviceActionService(AdbDeviceService adbDeviceService, StoragePaths storagePaths) {
        this.adbDeviceService = adbDeviceService;
        this.storagePaths = storagePaths;
    }

    public String saveLogs(String serial, String selectedFolder) {
        String appFlavour = adbDeviceService.getSafePathPackage(serial);
        adbDeviceService.saveLogs(serial, appFlavour, selectedFolder);
        return selectedFolder + "/logs/";
    }

    public void enableWifiDebugging(String serial, String ip) {
        adbDeviceService.startWifiDebugging(serial, ip);
    }

    public void disableWifiDebugging(String serial, String ip) {
        adbDeviceService.stopWifiDebugging(serial, ip);
    }

    public void reboot(String serial) {
        adbDeviceService.reboot(serial);
    }

    public File captureScreenshot(String serial, String deviceName) {
        String output = adbDeviceService.takeScreenshot(serial, "sdcard/", "screenshot.png");
        File screenshotDir = storagePaths.screenshotDir(deviceName);
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }
        adbDeviceService.pullFile(serial, output, screenshotDir.getPath());
        return screenshotDir;
    }

    public void enableFirebaseDebugging(String serial, String packageName) {
        adbDeviceService.enableAnalyticsDebug(serial, packageName);
    }

    public void uninstallApp(String serial, String packageName) {
        adbDeviceService.uninstallApp(serial, packageName);
    }

    public void openFolder(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            try {
                java.awt.Desktop.getDesktop().open(folder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Folder does not exist or is not a directory: " + folderPath);
        }
    }
}
