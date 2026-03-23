package androidtoolkit.app;

import androidtoolkit.domain.BuildSelectionState;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.DeviceGateway;
import androidtoolkit.ui.ConsoleView;
import androidtoolkit.ui.Device;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BuildInstaller {

    private final DeviceGateway deviceGateway;
    private final CommandExecutor commandExecutor;
    private final AtomicInteger runningTaskCount = new AtomicInteger();

    public BuildInstaller(DeviceGateway deviceGateway, CommandExecutor commandExecutor) {
        this.deviceGateway = deviceGateway;
        this.commandExecutor = commandExecutor;
    }

    public int installSelectedDevices(
            List<Device> devices,
            BuildSelectionState buildSelectionState,
            ConsoleView consoleView,
            TaskLauncher taskLauncher
    ) {
        int tasksStarted = 0;
        for (Device device : devices) {
            if (device.isSelectedForInstall()) {
                device.setRadioState(true);
                tasksStarted++;
                String command = "adb -s " + device.getSerial() + " install " + "\"" + buildSelectionState.getPrimaryBuildPath() + "\"";
                runningTaskCount.incrementAndGet();
                taskLauncher.launch(command);
                consoleView.appendText(device.getDeviceName() + " (" + device.getSerial() + "):" + "\n" + "App installed: " + buildSelectionState.getPrimaryBuildName());
            } else {
                device.setRadioState(false);
            }
        }
        return tasksStarted;
    }

    public int uninstallInstalledDevices(
            List<Device> devices,
            String buildName,
            ConsoleView consoleView,
            TaskLauncher taskLauncher
    ) {
        int tasksStarted = 0;
        for (Device device : devices) {
            if (device.isAppInstalled()) {
                tasksStarted++;
                String command = "adb -s " + device.getSerial() + " shell pm uninstall " + deviceGateway.getSafePathPackage(device.getSerial());
                runningTaskCount.incrementAndGet();
                taskLauncher.launch(command);
                consoleView.appendText(device.getDeviceName() + " (" + device.getSerial() + "):" + "\n" + "App removed: " + buildName);
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
