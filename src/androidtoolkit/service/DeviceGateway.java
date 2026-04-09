package androidtoolkit.service;

import java.util.ArrayList;

public interface DeviceGateway {

    ArrayList<String> getConnectedDevices();

    String getWlanIp(String id);

    String getMobileIp(String id);

    String getDeviceOSVersion(String id);

    String getDeviceName(String id);

    String getDeviceModel(String id);

    String getDeviceManufacturer(String id);

    void installApp(String id, String path);

    void saveAllLogs(String id, String pid, String newFolder);

    void saveLogs(String id, String appFlavour, String newFolder);

    void saveScreenRecordingLogs(String id, String pid, String deviceName, String name);

    void grantPermission(String id, String appPackage, String permission);

    void revokePermission(String id, String appPackage, String permission);

    void addToDeviceIdleWhitelist(String id, String appPackage);

    void removeFromDeviceIdleWhitelist(String id, String appPackage);

    void ignoreAutoRevokePermissions(String id, String appPackage);

    void resetAutoRevokePermissions(String id, String appPackage);

    String getPackageDump(String id, String appPackage);

    boolean isInDeviceIdleWhitelist(String id, String appPackage);

    String getAutoRevokePermissionsState(String id, String appPackage);

    void startWifiDebugging(String id, String ip);

    void stopWifiDebugging(String id, String ip);

    void enableAnalyticsDebug(String id, String installedPackage);

    ArrayList<String> getInstalledPackages(String id);

    String getSafePathPackage(String id);

    boolean uninstallApp(String id, String appPackage);

    void reboot(String id);

    String takeScreenshot(String id, String target, String fileName);

    boolean pullFile(String id, String source, String target);

    void deleteFile(String id, String target);
}
