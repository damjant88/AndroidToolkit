import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Util {

	private volatile boolean stopRequested = false;

	public String runCommand(String command) {
		String output = null;
		// ProcessBuilder does not read spaces hence need to do this
		String[] str = command.split(" ");
		List<String> commands = new ArrayList<>(Arrays.asList(str));
		try {
			ProcessBuilder pb = new ProcessBuilder(commands).redirectErrorStream(true);
			Process process = pb.start();
			output = new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
					.collect(Collectors.joining("\r\n"));
			if (!process.waitFor(3, TimeUnit.SECONDS)) {
				// if the process has not exited yet, destroy it forcefully
				process.destroyForcibly();
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return output.trim();
	}

	public String runLiveLogs(String command, String searchString) {
		StringBuilder output = new StringBuilder();

		String[] str = command.split(" ");
		List<String> commands = new ArrayList<>(Arrays.asList(str));
		try {
			ProcessBuilder pb = new ProcessBuilder(commands).redirectErrorStream(true);
			Process process = pb.start();

			// Clear stopRequested flag when starting tracking
			stopRequested = false;

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while (!stopRequested && (line = reader.readLine()) != null) {
					output.append(line).append(System.lineSeparator()); // Append line separator
					// Check if the line contains the specific string
//					System.out.println(line);
					if (line.contains(searchString)) {
						// Do whatever you need with the line
						System.out.println(line);
					}
				}
			}

			if (!process.waitFor(3, TimeUnit.SECONDS)) {
				// if the process has not exited yet, destroy it forcefully
				process.destroyForcibly();
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return output.toString().trim();
	}

	public void stopTracking() {
		stopRequested = true; // Set stopRequested flag to stop tracking
	}

	public void runCommandAndSave(String command, String fileName) {
        try {
			ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
			processBuilder.redirectOutput(new File(fileName));
			Process process = processBuilder.start();
			process.waitFor();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> getConnectedDevices() {
		ArrayList<String> devices = new ArrayList<>();
		String output = runCommand("adb devices");
		devices.addAll(Arrays.stream(output.split("\n")).map(String::trim).filter(line -> line.endsWith("device"))
				.map(line -> line.replace("device", "").trim()).collect(Collectors.toList()));
		return devices;

	}

	public String getWlanIp(String ID) {
		String ip = "";
		String output = runCommand("adb -s " + ID + " shell ip addr show wlan0");
		ip = Arrays.stream(output.split("\n")).map(String::trim)
				.filter(line -> line.startsWith("inet") && line.endsWith("wlan0"))
				.map(line -> line.replace("inet", "").trim()).map(line -> line.substring(0, line.indexOf("/")))
				.findFirst().orElse("");
		return ip;
	}

	public String getMobileIp(String ID) {
		String ip = "";
		String output = runCommand("adb -s " + ID + " shell ip addr show rmnet_data1");
		ip = Arrays.stream(output.split("\n")).map(String::trim)
				.filter(line -> line.startsWith("inet") && line.endsWith("rmnet_data1"))
				.map(line -> line.replace("inet", "").trim()).map(line -> line.substring(0, line.indexOf("/")))
				.findFirst().orElse("");
		return ip;
	}

	public String getDeviceOSVersion(String ID) {
		String os_version;
		os_version = runCommand("adb -s " + ID + " shell getprop ro.build.version.release");
		return os_version.trim();
	}

	public String getDeviceName(String ID) {
		return runCommand("adb -s " + ID + " shell getprop ro.product.name");
	}

	public String getDeviceModel(String ID) {
		String output;
		output = runCommand("adb -s " + ID + " shell getprop ro.product.model");
		return output.trim();
	}

	public String getDeviceManufacturer(String ID) {
		return runCommand("adb -s " + ID + " shell getprop ro.product.manufacturer");
	}

	public void installApp(String ID, String path) {
		runCommand("adb -s " + ID + " install -r -d " + path);
	}

	public void saveAllLogs(String ID, String pid, String newFolder) {
		LocalDate currentDate = LocalDate.now();
		String command = "adb -s " + ID + " logcat -d --pid=" + pid;
		String fileName = newFolder + "/" + "app-" + currentDate + ".log";
		runCommandAndSave(command, fileName);
	}

	public void saveLogs(String ID, String appFlavour, String newFolder) {
		runCommand("adb -s " + ID + " pull " + "sdcard/Android/data/" + appFlavour + "/files/logs/ "
				+ newFolder);
	}

	public void saveScreenRecordingLogs(String ID, String pid , String deviceName, String name) {
		LocalDate currentDate = LocalDate.now();
		String dateString = currentDate.toString();
		String command = "adb -s " + ID + " logcat -d --pid=" + pid;
		String fileName = "C:/AdbToolkit/Screen_Recordings/" + deviceName + "/" + dateString + "/" + name + ".log";
		runCommandAndSave(command, fileName);
	}

	public void startWifiDebugging(String ID, String IP) {
		runCommand("adb -s " + ID + " shell settings put global adb_wifi_enabled 1");
		runCommand("adb -s " + ID + " tcpip 5555");
		runCommand("adb -s " + ID + " connect " + IP);
	}

	public void stopWifiDebugging(String ID, String IP) {
		runCommand("adb -s " + ID + " disconnect " + IP);
	}

	public void enableAnalyticsDebug(String ID, String installedPackage) {
		runCommand("adb -s " + ID + " shell setprop debug.firebase.analytics.app " + installedPackage);
		runCommand("adb -s " + ID + " shell setprop log.tag.FA VERBOSE");
	}

	public ArrayList<String> getInstalledPackages(String ID) {
		ArrayList<String> packages = new ArrayList<>();
		String[] output = runCommand("adb -s " + ID + " shell pm list packages --user 0").split("\n");
		for (String packagedID : output) {
			packages.add(packagedID.replace("package:", "").trim());
		}
		return packages;
	}

	public String getSafePathPackage(String ID) {
		String installedPackage = "";
		ArrayList<String> installedPackages = getInstalledPackages(ID);
		for (String installedPackage2 : installedPackages) {
			if (installedPackage2.contains("safepath.family")
					|| installedPackage2.contains("securefamily")
					|| installedPackage2.contains("wavemarket")
					|| installedPackage2.contains("safeandfound")
					|| installedPackage2.contains("safefound")
					|| installedPackage2.contains("familycontrols")
					|| installedPackage2.contains("orangespain")
					|| installedPackage2.contains("dish")
					|| installedPackage2.contains("familymode")) {

				installedPackage = installedPackage2;
			}
		}
		return installedPackage;
	}

	public boolean checkIfInstalled(String ID) {
		String installedPackage = getSafePathPackage(ID);
		return installedPackage.equals("com.smithmicro.tmobile.familymode.test")
				|| installedPackage.equals("com.smithmicro.att.securefamily")
				|| installedPackage.equals("com.att.securefamilycompanion")
				|| installedPackage.equals("com.wavemarket.waplauncher")
				|| installedPackage.equals("com.smithmicro.safepath.family")
				|| installedPackage.equals("com.smithmicro.sprint.safeandfound.test")
				|| installedPackage.equals("com.sprint.safefound")
				|| installedPackage.equals("com.tmobile.familycontrols")
				|| installedPackage.equals("com.smithmicro.orangespain.test")
				|| installedPackage.equals("com.smithmicro.safepath.dish.test")
				|| installedPackage.equals("com.smithmicro.safepath.dish.kid.test")
				|| installedPackage.equals("com.smithmicro.safepath.family.child");
	}

	public boolean uninstallApp(String ID, String appPackage) {
		String output = runCommand("adb -s " + ID + " shell pm uninstall " + appPackage);
		if (output.contains("Success")) {
			return true;
		} else {
			return false;
		}
	}

	public void reboot(String ID) {
		runCommand("adb -s " + ID + " reboot");
	}

	public String takeScreenshot(String ID, String target, String fileName) {
		runCommand("adb -s " + ID + " shell screencap " + target + fileName);
		return runCommand("adb -s " + ID + " shell ls -t /sdcard/screenshot.png | grep " + fileName + " -m 1");
	}

	public boolean pullFile(String ID, String source, String target) {
		String output = runCommand("adb -s " + ID + " pull " + source + " " + target);
		return output.contains("file pulled");
	}

	public void deleteFile(String ID, String target) {
		runCommand("adb -s " + ID + " shell rm " + target);
	}

}
