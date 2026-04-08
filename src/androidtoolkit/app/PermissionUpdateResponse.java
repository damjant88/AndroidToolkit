package androidtoolkit.app;

public class PermissionUpdateResponse {

    private final PermissionUpdateResult updateResult;
    private final PermissionDialogState dialogState;

    public PermissionUpdateResponse(PermissionUpdateResult updateResult, PermissionDialogState dialogState) {
        this.updateResult = updateResult;
        this.dialogState = dialogState;
    }

    public PermissionUpdateResult getUpdateResult() {
        return updateResult;
    }

    public PermissionDialogState getDialogState() {
        return dialogState;
    }
}
