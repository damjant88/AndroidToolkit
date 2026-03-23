package androidtoolkit.ui;

import javax.swing.*;

public class DevicePanelState {

    private final ImageIcon icon;
    private final String labelText;
    private final boolean uninstallEnabled;
    private final boolean enableFirebaseEnabled;
    private final boolean saveLogsEnabled;
    private final boolean logLocationEnabled;
    private final boolean eventTrackerEnabled;
    private final boolean screenMirrorEnabled;
    private final boolean screenRecordingEnabled;
    private final boolean screenshotEnabled;
    private final boolean wifiDebugEnabled;
    private final boolean rebootEnabled;
    private final String wifiButtonText;

    public DevicePanelState(
            ImageIcon icon,
            String labelText,
            boolean uninstallEnabled,
            boolean enableFirebaseEnabled,
            boolean saveLogsEnabled,
            boolean logLocationEnabled,
            boolean eventTrackerEnabled,
            boolean screenMirrorEnabled,
            boolean screenRecordingEnabled,
            boolean screenshotEnabled,
            boolean wifiDebugEnabled,
            boolean rebootEnabled,
            String wifiButtonText
    ) {
        this.icon = icon;
        this.labelText = labelText;
        this.uninstallEnabled = uninstallEnabled;
        this.enableFirebaseEnabled = enableFirebaseEnabled;
        this.saveLogsEnabled = saveLogsEnabled;
        this.logLocationEnabled = logLocationEnabled;
        this.eventTrackerEnabled = eventTrackerEnabled;
        this.screenMirrorEnabled = screenMirrorEnabled;
        this.screenRecordingEnabled = screenRecordingEnabled;
        this.screenshotEnabled = screenshotEnabled;
        this.wifiDebugEnabled = wifiDebugEnabled;
        this.rebootEnabled = rebootEnabled;
        this.wifiButtonText = wifiButtonText;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getLabelText() {
        return labelText;
    }

    public boolean isUninstallEnabled() {
        return uninstallEnabled;
    }

    public boolean isEnableFirebaseEnabled() {
        return enableFirebaseEnabled;
    }

    public boolean isSaveLogsEnabled() {
        return saveLogsEnabled;
    }

    public boolean isLogLocationEnabled() {
        return logLocationEnabled;
    }

    public boolean isEventTrackerEnabled() {
        return eventTrackerEnabled;
    }

    public boolean isScreenMirrorEnabled() {
        return screenMirrorEnabled;
    }

    public boolean isScreenRecordingEnabled() {
        return screenRecordingEnabled;
    }

    public boolean isScreenshotEnabled() {
        return screenshotEnabled;
    }

    public boolean isWifiDebugEnabled() {
        return wifiDebugEnabled;
    }

    public boolean isRebootEnabled() {
        return rebootEnabled;
    }

    public String getWifiButtonText() {
        return wifiButtonText;
    }
}
