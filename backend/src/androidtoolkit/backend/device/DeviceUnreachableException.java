package androidtoolkit.backend.device;

/**
 * Thrown when a device command cannot be relayed because no agent is connected
 * for the target device. Maps to HTTP 503 (Service Unavailable) in the global
 * exception handler.
 */
public class DeviceUnreachableException extends RuntimeException {

    private final String serial;
    private final String reason;

    public DeviceUnreachableException(String serial, String reason) {
        super("Device unreachable: " + serial + " — " + reason);
        this.serial = serial;
        this.reason = reason;
    }

    public String getSerial() {
        return serial;
    }

    public String getReason() {
        return reason;
    }
}
