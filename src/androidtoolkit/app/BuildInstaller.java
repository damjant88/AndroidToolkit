package androidtoolkit.app;

import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.DeviceGateway;
import androidtoolkit.ui.ConsoleView;

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
            BuildInstallRequest request,
            ConsoleView consoleView,
            TaskLauncher taskLauncher
    ) {
        int tasksStarted = 0;
        for (DeviceTarget deviceTarget : request.getDeviceTargets()) {
            if (deviceTarget.isSelectedForInstall()) {
                tasksStarted++;
                String command = "adb -s " + deviceTarget.getSerial() + " install " + "\"" + request.getBuildPath() + "\"";
                runningTaskCount.incrementAndGet();
                taskLauncher.launch(command);
                consoleView.appendText(deviceTarget.getDeviceName() + " (" + deviceTarget.getSerial() + "):" + "\n" + "App installed: " + request.getBuildName());
            }
        }
        return tasksStarted;
    }

    public int uninstallInstalledDevices(
            BuildUninstallRequest request,
            ConsoleView consoleView,
            TaskLauncher taskLauncher
    ) {
        int tasksStarted = 0;
        for (DeviceTarget deviceTarget : request.getDeviceTargets()) {
            if (deviceTarget.isAppInstalled()) {
                tasksStarted++;
                String command = "adb -s " + deviceTarget.getSerial() + " shell pm uninstall " + deviceGateway.getSafePathPackage(deviceTarget.getSerial());
                runningTaskCount.incrementAndGet();
                taskLauncher.launch(command);
                consoleView.appendText(deviceTarget.getDeviceName() + " (" + deviceTarget.getSerial() + "):" + "\n" + "App removed: " + request.getBuildName());
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
