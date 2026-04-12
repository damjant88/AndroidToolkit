package androidtoolkit.ui;

import androidtoolkit.ui.ConsoleView;

import javax.swing.SwingWorker;

public class BuildCommandTask extends SwingWorker<String, Void> {

    private final String command;
    private final String successMessage;
    private final String failMessage;
    private final ConsoleView consoleView;
    private final BuildOperationCoordinator buildOperationCoordinator;
    private final BuildOperationCoordinator.BuildOperationUi buildOperationUi;

    public BuildCommandTask(
            String command,
            String successMessage,
            String failMessage,
            ConsoleView consoleView,
            BuildOperationCoordinator buildOperationCoordinator,
            BuildOperationCoordinator.BuildOperationUi buildOperationUi
    ) {
        this.command = command;
        this.successMessage = successMessage;
        this.failMessage = failMessage;
        this.consoleView = consoleView;
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
            consoleView.appendText(succeeded ? successMessage : failMessage);
        } catch (Exception e) {
            consoleView.appendText(failMessage);
        }
        buildOperationCoordinator.finishTask(buildOperationUi);
    }
}
