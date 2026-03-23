import java.io.File;
import java.io.IOException;

public class DeviceActionService {

    private final Util utility;
    private final StoragePaths storagePaths;

    public DeviceActionService(Util utility, StoragePaths storagePaths) {
        this.utility = utility;
        this.storagePaths = storagePaths;
    }

    public String saveLogs(String serial, String selectedFolder) {
        String appFlavour = utility.getSafePathPackage(serial);
        utility.saveLogs(serial, appFlavour, selectedFolder);
        return selectedFolder + "/logs/";
    }

    public void enableWifiDebugging(String serial, String ip) {
        utility.startWifiDebugging(serial, ip);
    }

    public void disableWifiDebugging(String serial, String ip) {
        utility.stopWifiDebugging(serial, ip);
    }

    public void reboot(String serial) {
        utility.reboot(serial);
    }

    public File captureScreenshot(String serial, String deviceName) {
        String output = utility.takeScreenshot(serial, "sdcard/", "screenshot.png");
        File screenshotDir = storagePaths.screenshotDir(deviceName);
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }
        utility.pullFile(serial, output, screenshotDir.getPath());
        return screenshotDir;
    }

    public void enableFirebaseDebugging(String serial, String packageName) {
        utility.enableAnalyticsDebug(serial, packageName);
    }

    public void uninstallApp(String serial, String packageName) {
        utility.uninstallApp(serial, packageName);
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
