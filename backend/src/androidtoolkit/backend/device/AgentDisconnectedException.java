package androidtoolkit.backend.device;

/**
 * Thrown when the WebSocket connection to an agent is lost after a command
 * has been sent but before an OperationResult is received. Maps to HTTP 502
 * (Bad Gateway) in the global exception handler.
 */
public class AgentDisconnectedException extends RuntimeException {

    private final String serial;
    private final String agentId;

    public AgentDisconnectedException(String serial, String agentId) {
        super("Agent " + agentId + " disconnected during command execution for device " + serial);
        this.serial = serial;
        this.agentId = agentId;
    }

    public String getSerial() {
        return serial;
    }

    public String getAgentId() {
        return agentId;
    }
}
