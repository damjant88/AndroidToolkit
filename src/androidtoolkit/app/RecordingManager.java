package androidtoolkit.app;

import androidtoolkit.domain.RecordingSession;

public class RecordingManager {

    private final DeviceOperations deviceOperations;

    public RecordingManager(DeviceOperations deviceOperations) {
        this.deviceOperations = deviceOperations;
    }

    public RecordingActionResponse startRecording(String serial, String deviceName, RecordingSession recordingSession) {
        StartScreenRecordingResult result = deviceOperations.startScreenRecording(
                new StartScreenRecordingRequest(serial, deviceName, recordingSession)
        );
        return new RecordingActionResponse(
                result.isStarted(),
                result.getMessage(),
                result.getButtonText(),
                ""
        );
    }

    public RecordingActionResponse stopRecording(String serial, String deviceName, String pid, RecordingSession recordingSession) throws InterruptedException {
        StopScreenRecordingResult result = deviceOperations.stopScreenRecording(
                new StopScreenRecordingRequest(serial, deviceName, pid, recordingSession)
        );
        return new RecordingActionResponse(
                result.isStopped(),
                result.getMessage(),
                result.getButtonText(),
                result.getRecordingLocation()
        );
    }

    public RecordingActionResponse noActiveRecording() {
        return new RecordingActionResponse(
                false,
                "No active recording!",
                "Start Record",
                ""
        );
    }
}
