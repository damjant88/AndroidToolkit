package androidtoolkit.domain.agent;

import androidtoolkit.domain.DeviceInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * Messages sent from the Agent to the Server via WebSocket.
 * Each message reports the result of an ADB operation or streams
 * real-time data from a connected device.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AgentMessage.LogcatLine.class, name = "LogcatLine"),
    @JsonSubTypes.Type(value = AgentMessage.DeviceList.class, name = "DeviceList"),
    @JsonSubTypes.Type(value = AgentMessage.OperationResult.class, name = "OperationResult"),
    @JsonSubTypes.Type(value = AgentMessage.LogArchiveReady.class, name = "LogArchiveReady")
})
public sealed interface AgentMessage {

    /**
     * A single logcat line streamed from a device.
     *
     * @param serial    the device serial number producing the line
     * @param line      the raw logcat line content
     * @param timestamp the epoch millisecond timestamp when the line was captured
     */
    record LogcatLine(String serial, String line, long timestamp) implements AgentMessage {}

    /**
     * The list of devices currently connected to the Agent.
     *
     * @param devices the connected device information
     */
    record DeviceList(List<DeviceInfo> devices) implements AgentMessage {}

    /**
     * The result of an operation requested by the Server.
     *
     * @param requestId the identifier of the original request
     * @param success   whether the operation completed successfully
     * @param detail    additional detail or error message
     */
    record OperationResult(String requestId, boolean success, String detail) implements AgentMessage {}

    /**
     * Notification that a log archive is ready for upload.
     *
     * @param serial    the device serial number the logs were pulled from
     * @param localPath the local filesystem path to the archive
     */
    record LogArchiveReady(String serial, String localPath) implements AgentMessage {}
}
