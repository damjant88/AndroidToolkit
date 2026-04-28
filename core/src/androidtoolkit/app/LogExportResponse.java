package androidtoolkit.app;

public class LogExportResponse {

    private final String selectedFolder;
    private final String exportedLogsFolder;
    private final String message;

    public LogExportResponse(String selectedFolder, String exportedLogsFolder, String message) {
        this.selectedFolder = selectedFolder;
        this.exportedLogsFolder = exportedLogsFolder;
        this.message = message;
    }

    public String getSelectedFolder() {
        return selectedFolder;
    }

    public String getExportedLogsFolder() {
        return exportedLogsFolder;
    }

    public String getMessage() {
        return message;
    }
}
