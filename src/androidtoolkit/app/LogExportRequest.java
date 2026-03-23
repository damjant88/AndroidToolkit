package androidtoolkit.app;

public class LogExportRequest {

    private final String serial;
    private final String targetFolder;

    public LogExportRequest(String serial, String targetFolder) {
        this.serial = serial;
        this.targetFolder = targetFolder;
    }

    public String getSerial() {
        return serial;
    }

    public String getTargetFolder() {
        return targetFolder;
    }
}
