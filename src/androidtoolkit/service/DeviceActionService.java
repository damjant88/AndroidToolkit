package androidtoolkit.service;

import java.io.File;
import java.io.IOException;

public class DeviceActionService {

    private final DeviceGateway deviceGateway;
    private final StoragePaths storagePaths;

    public DeviceActionService(DeviceGateway deviceGateway, StoragePaths storagePaths) {
        this.deviceGateway = deviceGateway;
        this.storagePaths = storagePaths;
    }

    public String saveLogs(String serial, String selectedFolder) {
        String appFlavour = deviceGateway.getSafePathPackage(serial);
        deviceGateway.saveLogs(serial, appFlavour, selectedFolder);
        return selectedFolder + "/logs/";
    }

    public void enableWifiDebugging(String serial, String ip) {
        deviceGateway.startWifiDebugging(serial, ip);
    }

    public void disableWifiDebugging(String serial, String ip) {
        deviceGateway.stopWifiDebugging(serial, ip);
    }

    public void reboot(String serial) {
        deviceGateway.reboot(serial);
    }

    public File captureScreenshot(String serial, String deviceName) {
        String output = deviceGateway.takeScreenshot(serial, "sdcard/", "screenshot.png");
        File screenshotDir = storagePaths.screenshotDir(deviceName);
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }
        deviceGateway.pullFile(serial, output, screenshotDir.getPath());
        return screenshotDir;
    }

    public void enableFirebaseDebugging(String serial, String packageName) {
        deviceGateway.enableAnalyticsDebug(serial, packageName);
    }

    public void uninstallApp(String serial, String packageName) {
        deviceGateway.uninstallApp(serial, packageName);
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
