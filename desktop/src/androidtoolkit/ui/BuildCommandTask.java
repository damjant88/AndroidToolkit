package androidtoolkit.ui;

import androidtoolkit.app.BuildOutputLog;

import javax.swing.SwingWorker;

public class BuildCommandTask extends SwingWorker<String, Void> {

    private final String command;
    private final String successMessage;
    private final String failMessage;
    private final BuildOutputLog outputLog;
    private final BuildOperationCoordinator buildOperationCoordinator;
    private final BuildOperationCoordinator.BuildOperationUi buildOperationUi;

    public BuildCommandTask(
            String command,
            String successMessage,
            String failMessage,
            BuildOutputLog outputLog,
            BuildOperationCoordinator buildOperationCoordinator,
            BuildOperationCoordinator.BuildOperationUi buildOperationUi
    ) {
        this.command = command;
        this.successMessage = successMessage;
        this.failMessage = failMessage;
        this.outputLog = outputLog;
        this.buildOperationCoordinator = buildOperationCoordinator;
        this.buildOperationUi = buildOperationUi;
    }

    @Override
    public String doInBackground() {
        // Run the adb command and return its output so we can check success/failure
        return buildOperationCoordinator.runCommand(command);
    }

    @Override
    public void done() {
        try {
            // Check the actual adb output to decide which message to show
            String output = get();
            boolean succeeded = output != null && output.contains("Success");
            outputLog.appendText(succeeded ? successMessage : failMessage);
        } catch (Exception e) {
            outputLog.appendText(failMessage);
        }
        buildOperationCoordinator.finishTask(buildOperationUi);
    }
}
