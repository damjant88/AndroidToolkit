package androidtoolkit.app;

public class LogExportResult {

    private final String exportedFolder;

    public LogExportResult(String exportedFolder) {
        this.exportedFolder = exportedFolder;
    }

    public String getExportedFolder() {
        return exportedFolder;
    }
}
