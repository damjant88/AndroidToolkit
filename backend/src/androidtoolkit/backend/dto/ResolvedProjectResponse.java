package androidtoolkit.backend.dto;

public record ResolvedProjectResponse(Long id, String name, String remoteApkLocation, String localApkFolder, boolean overridden) {}
