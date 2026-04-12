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
                String startMessage = deviceTarget.getDeviceName() + " (" + deviceTarget.getSerial() + "):" + "\n" + "Installing: " + request.getBuildName();
                String successMessage = deviceTarget.getDeviceName() + " (" + deviceTarget.getSerial() + "):" + "\n" + "App installed: " + request.getBuildName();
                String failMessage = deviceTarget.getDeviceName() + " (" + deviceTarget.getSerial() + "):" + "\n" + "Install failed: " + request.getBuildName();
                consoleView.appendText(startMessage);
                taskLauncher.launch(command, successMessage, failMessage, consoleView);
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
                String startMessage = deviceTarget.getDeviceName() + " (" + deviceTarget.getSerial() + "):" + "\n" + "App uninstall started: " + request.getBuildName();
                String successMessage = deviceTarget.getDeviceName() + " (" + deviceTarget.getSerial() + "):" + "\n" + "App removed: " + request.getBuildName();
                String failMessage = deviceTarget.getDeviceName() + " (" + deviceTarget.getSerial() + "):" + "\n" + "Uninstall failed: " + request.getBuildName();
                consoleView.appendText(startMessage);
                taskLauncher.launch(command, successMessage, failMessage, consoleView);
            }
        }
        return tasksStarted;
    }

    // Returns the adb output so the caller can check for success/failure
    public String runCommand(String command) {
        return commandExecutor.runCommand(command);
    }

    public int finishTask() {
        return runningTaskCount.decrementAndGet();
    }

    public interface TaskLauncher {
        // successMessage: logged if adb output contains "Success"
        // failMessage: logged otherwise
        void launch(String command, String successMessage, String failMessage, ConsoleView consoleView);
    }
}
