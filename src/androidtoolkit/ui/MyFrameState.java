package androidtoolkit.ui;

public class MyFrameState {

    private final boolean installEnabled;
    private final boolean uninstallAllEnabled;
    private final int windowWidth;

    public MyFrameState(boolean installEnabled, boolean uninstallAllEnabled, int windowWidth) {
        this.installEnabled = installEnabled;
        this.uninstallAllEnabled = uninstallAllEnabled;
        this.windowWidth = windowWidth;
    }

    public boolean isInstallEnabled() {
        return installEnabled;
    }

    public boolean isUninstallAllEnabled() {
        return uninstallAllEnabled;
    }

    public int getWindowWidth() {
        return windowWidth;
    }
}
