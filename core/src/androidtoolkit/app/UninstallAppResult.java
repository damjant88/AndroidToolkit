package androidtoolkit.app;

public class UninstallAppResult {

    private final boolean uninstalled;
    private final String message;

    public UninstallAppResult(boolean uninstalled, String message) {
        this.uninstalled = uninstalled;
        this.message = message;
    }

    public boolean isUninstalled() {
        return uninstalled;
    }

    public String getMessage() {
        return message;
    }
}
