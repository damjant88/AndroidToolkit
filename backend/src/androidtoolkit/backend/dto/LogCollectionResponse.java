package androidtoolkit.backend.dto;

public record LogCollectionResponse(
    String localPath,
    String exportedLogsFolder,
    String message,
    String sharedStoragePath,
    Long projectId
) {}
