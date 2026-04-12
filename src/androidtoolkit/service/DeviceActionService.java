package androidtoolkit.service;

import java.io.File;

public class DeviceActionService {

    private final DeviceGateway deviceGateway;
    private final StorageService storageService;
    private final HostToolsGateway hostToolsGateway;

    public DeviceActionService(DeviceGateway deviceGateway, StorageService storageService, HostToolsGateway hostToolsGateway) {
        this.deviceGateway = deviceGateway;
        this.storageService = storageService;
        this.hostToolsGateway = hostToolsGateway;
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
        File screenshotDir = storageService.screenshotDir(deviceName);
        storageService.ensureDirectoryExists(screenshotDir);
        deviceGateway.pullFile(serial, output, screenshotDir.getPath());
        return screenshotDir;
    }

    public void enableFirebaseDebugging(String serial, String packageName) {
        deviceGateway.enableAnalyticsDebug(serial, packageName);
    }

    // Return the result so the caller knows if uninstall actually succeeded
    public boolean uninstallApp(String serial, String packageName) {
        return deviceGateway.uninstallApp(serial, packageName);
    }

    public void openFolder(String folderPath) {
        File folder = new File(folderPath);
        hostToolsGateway.openFolder(folder);
    }
}
