package androidtoolkit.app;

import androidtoolkit.domain.RecordingSession;

public class StartScreenRecordingRequest {

    private final String serial;
    private final String deviceName;
    private final RecordingSession recordingSession;

    public StartScreenRecordingRequest(String serial, String deviceName, RecordingSession recordingSession) {
        this.serial = serial;
        this.deviceName = deviceName;
        this.recordingSession = recordingSession;
    }

    public String getSerial() {
        return serial;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public RecordingSession getRecordingSession() {
        return recordingSession;
    }
}
