package androidtoolkit.service;

import java.util.ArrayList;

public class Util {

	private final CommandExecutor commandExecutor;
	private final AdbDeviceService adbDeviceService;

	public String runCommand(String command) {
		return commandExecutor.runCommand(command);
	}

	public Util() {
		this.commandExecutor = new CommandExecutor();
		this.adbDeviceService = new AdbDeviceService(commandExecutor, new PackageClassifier(), new StoragePaths());
	}

	public String runLiveLogs(String command, String searchString) {
		return commandExecutor.runLiveLogs(command, searchString);
	}

	public void stopTracking() {
		commandExecutor.stopTracking();
	}

	public void runCommandAndSave(String command, String fileName) {
		commandExecutor.runCommandAndSave(command, fileName);
	}

	public ArrayList<String> getConnectedDevices() {
		return adbDeviceService.getConnectedDevices();
	}

	public String getWlanIp(String ID) {
		return adbDeviceService.getWlanIp(ID);
	}

	public String getMobileIp(String ID) {
		return adbDeviceService.getMobileIp(ID);
	}

	public String getDeviceOSVersion(String ID) {
		return adbDeviceService.getDeviceOSVersion(ID);
	}

	public String getDeviceName(String ID) {
		return adbDeviceService.getDeviceName(ID);
	}

	public String getDeviceModel(String ID) {
		return adbDeviceService.getDeviceModel(ID);
	}

	public String getDeviceManufacturer(String ID) {
		return adbDeviceService.getDeviceManufacturer(ID);
	}

	public void installApp(String ID, String path) {
		adbDeviceService.installApp(ID, path);
	}

	public void saveAllLogs(String ID, String pid, String newFolder) {
		adbDeviceService.saveAllLogs(ID, pid, newFolder);
	}

	public void saveLogs(String ID, String appFlavour, String newFolder) {
		adbDeviceService.saveLogs(ID, appFlavour, newFolder);
	}

	public void saveScreenRecordingLogs(String ID, String pid , String deviceName, String name) {
		adbDeviceService.saveScreenRecordingLogs(ID, pid, deviceName, name);
	}

	public void startWifiDebugging(String ID, String IP) {
		adbDeviceService.startWifiDebugging(ID, IP);
	}

	public void stopWifiDebugging(String ID, String IP) {
		adbDeviceService.stopWifiDebugging(ID, IP);
	}

	public void enableAnalyticsDebug(String ID, String installedPackage) {
		adbDeviceService.enableAnalyticsDebug(ID, installedPackage);
	}

	public ArrayList<String> getInstalledPackages(String ID) {
		return adbDeviceService.getInstalledPackages(ID);
	}

	public String getSafePathPackage(String ID) {
		return adbDeviceService.getSafePathPackage(ID);
	}

	public boolean checkIfInstalled(String ID) {
		return adbDeviceService.checkIfInstalled(ID);
	}

	public boolean uninstallApp(String ID, String appPackage) {
		return adbDeviceService.uninstallApp(ID, appPackage);
	}

	public void reboot(String ID) {
		adbDeviceService.reboot(ID);
	}

	public String takeScreenshot(String ID, String target, String fileName) {
		return adbDeviceService.takeScreenshot(ID, target, fileName);
	}

	public boolean pullFile(String ID, String source, String target) {
		return adbDeviceService.pullFile(ID, source, target);
	}

	public void deleteFile(String ID, String target) {
		adbDeviceService.deleteFile(ID, target);
	}

}
