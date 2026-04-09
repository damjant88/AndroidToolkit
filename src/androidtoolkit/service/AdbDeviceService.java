package androidtoolkit.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdbDeviceService implements DeviceGateway {

    private final CommandExecutor commandExecutor;
    private final PackageClassifier packageClassifier;
    private final StoragePaths storagePaths;

    public AdbDeviceService(CommandExecutor commandExecutor, PackageClassifier packageClassifier, StoragePaths storagePaths) {
        this.commandExecutor = commandExecutor;
        this.packageClassifier = packageClassifier;
        this.storagePaths = storagePaths;
    }

    public ArrayList<String> getConnectedDevices() {
        ArrayList<String> devices = new ArrayList<>();
        String output = commandExecutor.runCommand("adb devices");
        devices.addAll(Arrays.stream(output.split("\n")).map(String::trim).filter(line -> line.endsWith("device"))
                .map(line -> line.replace("device", "").trim()).collect(Collectors.toList()));
        return devices;
    }

    public String getWlanIp(String id) {
        String output = commandExecutor.runCommand("adb -s " + id + " shell ip addr show wlan0");
        return Arrays.stream(output.split("\n")).map(String::trim)
                .filter(line -> line.startsWith("inet") && line.endsWith("wlan0"))
                .map(line -> line.replace("inet", "").trim()).map(line -> line.substring(0, line.indexOf("/")))
                .findFirst().orElse("");
    }

    public String getMobileIp(String id) {
        String output = commandExecutor.runCommand("adb -s " + id + " shell ip addr show rmnet_data1");
        return Arrays.stream(output.split("\n")).map(String::trim)
                .filter(line -> line.startsWith("inet") && line.endsWith("rmnet_data1"))
                .map(line -> line.replace("inet", "").trim()).map(line -> line.substring(0, line.indexOf("/")))
                .findFirst().orElse("");
    }

    public String getDeviceOSVersion(String id) {
        return commandExecutor.runCommand("adb -s " + id + " shell getprop ro.build.version.release").trim();
    }

    public String getDeviceName(String id) {
        return commandExecutor.runCommand("adb -s " + id + " shell getprop ro.product.name");
    }

    public String getDeviceModel(String id) {
        return commandExecutor.runCommand("adb -s " + id + " shell getprop ro.product.model").trim();
    }

    public String getDeviceManufacturer(String id) {
        return commandExecutor.runCommand("adb -s " + id + " shell getprop ro.product.manufacturer");
    }

    public void installApp(String id, String path) {
        commandExecutor.runCommand("adb -s " + id + " install -r -d \"" + path + "\"");
    }

    public void saveAllLogs(String id, String pid, String newFolder) {
        LocalDate currentDate = LocalDate.now();
        String command = "adb -s " + id + " logcat -d --pid=" + pid;
        String fileName = newFolder + "/" + "app-" + currentDate + ".log";
        commandExecutor.runCommandAndSave(command, fileName);
    }

    public void saveLogs(String id, String appFlavour, String newFolder) {
        commandExecutor.runCommand("adb -s " + id + " pull " + "sdcard/Android/data/" + appFlavour + "/files/logs/ "
                + "\"" + newFolder + "\"");
    }

    public void saveScreenRecordingLogs(String id, String pid, String deviceName, String name) {
        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString();
        String command = "adb -s " + id + " logcat -d --pid=" + pid;
        String fileName = storagePaths.recordingDir(deviceName, dateString).getPath() + "/" + name + ".log";
        commandExecutor.runCommandAndSave(command, fileName);
    }

    public void grantPermission(String id, String appPackage, String permission) {
        commandExecutor.runCommand("adb -s " + id + " shell pm grant " + appPackage + " " + permission);
    }

    public void revokePermission(String id, String appPackage, String permission) {
        commandExecutor.runCommand("adb -s " + id + " shell pm revoke " + appPackage + " " + permission);
    }

    public void addToDeviceIdleWhitelist(String id, String appPackage) {
        commandExecutor.runCommand("adb -s " + id + " shell cmd deviceidle whitelist +" + appPackage);
    }

    public void removeFromDeviceIdleWhitelist(String id, String appPackage) {
        commandExecutor.runCommand("adb -s " + id + " shell cmd deviceidle whitelist -" + appPackage);
    }

    public void ignoreAutoRevokePermissions(String id, String appPackage) {
        commandExecutor.runCommand("adb -s " + id + " shell cmd appops set " + appPackage + " AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore");
    }

    public void resetAutoRevokePermissions(String id, String appPackage) {
        commandExecutor.runCommand("adb -s " + id + " shell cmd appops set " + appPackage + " AUTO_REVOKE_PERMISSIONS_IF_UNUSED default");
    }

    public String getPackageDump(String id, String appPackage) {
        return commandExecutor.runCommand("adb -s " + id + " shell dumpsys package " + appPackage);
    }

    public boolean isInDeviceIdleWhitelist(String id, String appPackage) {
        String whitelistOutput = commandExecutor.runCommand("adb -s " + id + " shell dumpsys deviceidle whitelist");
        return whitelistOutput.contains(appPackage);
    }

    public String getAutoRevokePermissionsState(String id, String appPackage) {
        return commandExecutor.runCommand("adb -s " + id + " shell cmd appops get " + appPackage + " AUTO_REVOKE_PERMISSIONS_IF_UNUSED");
    }

    public void startWifiDebugging(String id, String ip) {
        commandExecutor.runCommand("adb -s " + id + " shell settings put global adb_wifi_enabled 1");
        commandExecutor.runCommand("adb -s " + id + " tcpip 5555");
        commandExecutor.runCommand("adb -s " + id + " connect " + ip);
    }

    public void stopWifiDebugging(String id, String ip) {
        commandExecutor.runCommand("adb -s " + id + " disconnect " + ip);
    }

    public void enableAnalyticsDebug(String id, String installedPackage) {
        commandExecutor.runCommand("adb -s " + id + " shell setprop debug.firebase.analytics.app " + installedPackage);
        commandExecutor.runCommand("adb -s " + id + " shell setprop log.tag.FA VERBOSE");
    }

    public ArrayList<String> getInstalledPackages(String id) {
        ArrayList<String> packages = new ArrayList<>();
        String[] output = commandExecutor.runCommand("adb -s " + id + " shell pm list packages --user 0").split("\n");
        for (String packagedId : output) {
            packages.add(packagedId.replace("package:", "").trim());
        }
        return packages;
    }

    public String getSafePathPackage(String id) {
        List<String> installedPackages = getInstalledPackages(id);
        return packageClassifier.detectSafePathPackage(installedPackages);
    }

    public boolean uninstallApp(String id, String appPackage) {
        commandExecutor.runCommand("adb -s " + id + " shell pm clear " + appPackage);
        commandExecutor.runCommand("adb -s " + id + " shell pm disable-user --user 0 " + appPackage);
        String output = commandExecutor.runCommand("adb -s " + id + " shell pm uninstall " + appPackage);
        return output.contains("Success");
    }

    public void reboot(String id) {
        commandExecutor.runCommand("adb -s " + id + " reboot");
    }

    public String takeScreenshot(String id, String target, String fileName) {
        commandExecutor.runCommand("adb -s " + id + " shell screencap " + target + fileName);
        return commandExecutor.runCommand("adb -s " + id + " shell ls -t /sdcard/screenshot.png | grep " + fileName + " -m 1");
    }

    public boolean pullFile(String id, String source, String target) {
        String output = commandExecutor.runCommand("adb -s " + id + " pull " + source + " \"" + target + "\"");
        return output.contains("file pulled");
    }

    public void deleteFile(String id, String target) {
        commandExecutor.runCommand("adb -s " + id + " shell rm " + target);
    }
}
