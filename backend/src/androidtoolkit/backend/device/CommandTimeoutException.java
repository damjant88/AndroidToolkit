package androidtoolkit.backend.device;

/**
 * Thrown when an agent does not respond with an OperationResult within the
 * configured timeout period. Maps to HTTP 504 (Gateway Timeout) in the global
 * exception handler.
 */
public class CommandTimeoutException extends RuntimeException {

    private final String serial;
    private final long timeoutSeconds;

    public CommandTimeoutException(String serial, long timeoutSeconds) {
        super("Command timed out for device " + serial + " after " + timeoutSeconds + " seconds");
        this.serial = serial;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getSerial() {
        return serial;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
