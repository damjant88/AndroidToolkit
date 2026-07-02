package androidtoolkit.backend.dto;

import java.time.Instant;

public record ProjectResponse(Long id, String name, String remoteApkLocation, String localApkFolder, String localLogFolder, String figmaLink, Instant createdAt) {}
