package androidtoolkit.ui;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.DeviceGateway;

public class DeviceCommandCoordinator {

    private final DeviceGateway deviceGateway;
    private final CommandExecutor commandExecutor;
    private final AtomicInteger runningTaskCount = new AtomicInteger();

    public DeviceCommandCoordinator(DeviceGateway deviceGateway, CommandExecutor commandExecutor) {
        this.deviceGateway = deviceGateway;
        this.commandExecutor = commandExecutor;
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
                String command = "adb -s " + device.serial + " shell pm uninstall " + deviceGateway.getSafePathPackage(device.serial);
                runningTaskCount.incrementAndGet();
                taskLauncher.launch(command);
                consoleView.appendText(device.deviceName + " (" + device.serial + "):" + "\n" + "App removed: " + buildName);
            }
        }
        return tasksStarted;
    }

    public void runCommand(String command) {
        commandExecutor.runCommand(command);
    }

    public int finishTask() {
        return runningTaskCount.decrementAndGet();
    }

    public interface TaskLauncher {
        void launch(String command);
    }
}
