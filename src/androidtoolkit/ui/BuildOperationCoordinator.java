package androidtoolkit.ui;

import androidtoolkit.app.BuildInstallRequest;
import androidtoolkit.app.BuildInstaller;
import androidtoolkit.app.BuildUninstallRequest;

import java.awt.Color;

public class BuildOperationCoordinator {

    private static final Color DEFAULT_PROGRESS_BACKGROUND = new Color(238, 238, 238);
    private static final BuildProgressState INSTALLING_STATE = new BuildProgressState("Installing...", true, Color.WHITE);
    private static final BuildProgressState UNINSTALLING_STATE = new BuildProgressState("Uninstalling...", true, DEFAULT_PROGRESS_BACKGROUND);
    private static final BuildProgressState WAITING_STATE = new BuildProgressState("Waiting for build...", false, DEFAULT_PROGRESS_BACKGROUND);
    private static final BuildProgressState DONE_STATE = new BuildProgressState("Done!", false, Color.green);

    private final BuildInstaller buildInstaller;

    public BuildOperationCoordinator(BuildInstaller buildInstaller) {
        this.buildInstaller = buildInstaller;
    }

    public void startInstall(BuildInstallRequest request, ConsoleView consoleView, BuildOperationUi ui) {
        ui.applyProgressState(INSTALLING_STATE);
        ui.setInstallEnabled(false);

        int tasksStarted = buildInstaller.installSelectedDevices(
                request,
                consoleView,
                command -> startTask(command, ui)
        );

        if (tasksStarted > 0) {
            ui.setUninstallAllEnabled(true);
        } else {
            ui.applyProgressState(WAITING_STATE);
            ui.applyCurrentFrameState();
        }
    }

    public void startUninstall(BuildUninstallRequest request, ConsoleView consoleView, BuildOperationUi ui) {
        ui.applyProgressState(UNINSTALLING_STATE);
        ui.setUninstallAllEnabled(false);

        int tasksStarted = buildInstaller.uninstallInstalledDevices(
                request,
                consoleView,
                command -> startTask(command, ui)
        );

        if (tasksStarted == 0) {
            ui.applyProgressState(DONE_STATE);
            ui.applyCurrentFrameState();
        }
    }

    public void runCommand(String command) {
        buildInstaller.runCommand(command);
    }

    public void finishTask(BuildOperationUi ui) {
        if (buildInstaller.finishTask() == 0) {
            ui.applyProgressState(DONE_STATE);
            ui.refreshDevices();
            ui.applyCurrentFrameState();
        }
    }

    private void startTask(String command, BuildOperationUi ui) {
        BuildCommandTask task = new BuildCommandTask(command, this, ui);
        task.execute();
    }

    public interface BuildOperationUi {
        void applyProgressState(BuildProgressState progressState);
        void setInstallEnabled(boolean enabled);
        void setUninstallAllEnabled(boolean enabled);
        void refreshDevices();
        void applyCurrentFrameState();
    }
}
