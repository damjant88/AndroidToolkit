package androidtoolkit.app;

public class WifiDebugResult {

    private final boolean wifiConnectionRequired;
    private final boolean wifiEnabled;
    private final String message;
    private final String buttonText;

    public WifiDebugResult(boolean wifiConnectionRequired, boolean wifiEnabled, String message, String buttonText) {
        this.wifiConnectionRequired = wifiConnectionRequired;
        this.wifiEnabled = wifiEnabled;
        this.message = message;
        this.buttonText = buttonText;
    }

    public boolean isWifiConnectionRequired() {
        return wifiConnectionRequired;
    }

    public boolean isWifiEnabled() {
        return wifiEnabled;
    }

    public String getMessage() {
        return message;
    }

    public String getButtonText() {
        return buttonText;
    }
}
