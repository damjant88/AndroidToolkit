package androidtoolkit.app;

import androidtoolkit.domain.RecordingSession;

public class StopScreenRecordingRequest {

    private final String serial;
    private final String deviceName;
    private final String pid;
    private final RecordingSession recordingSession;

    public StopScreenRecordingRequest(String serial, String deviceName, String pid, RecordingSession recordingSession) {
        this.serial = serial;
        this.deviceName = deviceName;
        this.pid = pid;
        this.recordingSession = recordingSession;
    }

    public String getSerial() {
        return serial;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getPid() {
        return pid;
    }

    public RecordingSession getRecordingSession() {
        return recordingSession;
    }
}
