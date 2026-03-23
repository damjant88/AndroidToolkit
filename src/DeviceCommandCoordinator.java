import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DeviceCommandCoordinator {

    private final Util utility;
    private final AtomicInteger runningTaskCount = new AtomicInteger();

    public DeviceCommandCoordinator(Util utility) {
        this.utility = utility;
    }

    public int startInstallTasks(List<Device> devices, String buildPath, String buildName, ConsoleView consoleView, TaskLauncher taskLauncher) {
        int tasksStarted = 0;
        for (Device device : devices) {
            if (device.radio.isSelected()) {
                device.radioState = true;
                tasksStarted++;
                String command = "adb -s " + device.serial + " install " + "\"" + buildPath + "\"";
                runningTaskCount.incrementAndGet();
                taskLauncher.launch(command);
                consoleView.appendText(device.deviceName + " (" + device.serial + "):" + "\n" + "App installed: " + buildName);
            } else {
                device.radioState = false;
            }
        }
        return tasksStarted;
    }

    public int startUninstallTasks(List<Device> devices, String buildName, ConsoleView consoleView, TaskLauncher taskLauncher) {
        int tasksStarted = 0;
        for (Device device : devices) {
            if (device.appIsInstalled) {
                tasksStarted++;
                String command = "adb -s " + device.serial + " shell pm uninstall " + utility.getSafePathPackage(device.serial);
                runningTaskCount.incrementAndGet();
                taskLauncher.launch(command);
                consoleView.appendText(device.deviceName + " (" + device.serial + "):" + "\n" + "App removed: " + buildName);
            }
        }
        return tasksStarted;
    }

    public void runCommand(String command) {
        utility.runCommand(command);
    }

    public int finishTask() {
        return runningTaskCount.decrementAndGet();
    }

    public interface TaskLauncher {
        void launch(String command);
    }
}
