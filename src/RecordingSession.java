import java.util.concurrent.atomic.AtomicBoolean;

public class RecordingSession {

    private String recordingFileName;
    private String recordingLocation;
    private Process recordingProcess;
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

    public String getRecordingFileName() {
        return recordingFileName;
    }

    public void setRecordingFileName(String recordingFileName) {
        this.recordingFileName = recordingFileName;
    }

    public String getRecordingLocation() {
        return recordingLocation;
    }

    public void setRecordingLocation(String recordingLocation) {
        this.recordingLocation = recordingLocation;
    }

    public Process getRecordingProcess() {
        return recordingProcess;
    }

    public void setRecordingProcess(Process recordingProcess) {
        this.recordingProcess = recordingProcess;
    }

    public AtomicBoolean getRecordingInProgress() {
        return recordingInProgress;
    }
}
