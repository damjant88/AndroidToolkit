package androidtoolkit.ui;

import javax.swing.SwingWorker;

public class BuildCommandTask extends SwingWorker<Void, Void> {

    private final String command;
    private final BuildOperationCoordinator buildOperationCoordinator;
    private final BuildOperationCoordinator.BuildOperationUi buildOperationUi;

    public BuildCommandTask(
            String command,
            BuildOperationCoordinator buildOperationCoordinator,
            BuildOperationCoordinator.BuildOperationUi buildOperationUi
    ) {
        this.command = command;
        this.buildOperationCoordinator = buildOperationCoordinator;
        this.buildOperationUi = buildOperationUi;
    }

    @Override
    public Void doInBackground() {
        buildOperationCoordinator.runCommand(command);
        return null;
    }

    @Override
    public void done() {
        buildOperationCoordinator.finishTask(buildOperationUi);
    }
}
