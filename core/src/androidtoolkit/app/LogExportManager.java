package androidtoolkit.app;

public class LogExportManager {

    private final LogExporter logExporter;

    public LogExportManager(LogExporter logExporter) {
        this.logExporter = logExporter;
    }

    public LogExportResponse exportDeviceLogs(String serial, String deviceName, String targetFolder) {
        String exportedLogsFolder = logExporter.exportDeviceLogs(new LogExportRequest(serial, targetFolder)).getExportedFolder();
        return new LogExportResponse(
                targetFolder,
                exportedLogsFolder,
                "SP logs from " + deviceName + " are saved to " + targetFolder
        );
    }
}
