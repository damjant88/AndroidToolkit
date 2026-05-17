package androidtoolkit.backend.device;

/**
 * Thrown when a command request specifies a command type that is not in the
 * supported set. Maps to HTTP 400 (Bad Request) in the global exception handler.
 */
public class UnsupportedCommandException extends RuntimeException {

    private final String commandType;

    public UnsupportedCommandException(String commandType) {
        super("Unsupported command type: " + commandType);
        this.commandType = commandType;
    }

    public String getCommandType() {
        return commandType;
    }
}
