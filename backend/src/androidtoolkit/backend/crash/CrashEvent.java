package androidtoolkit.backend.crash;

import java.time.Instant;

/**
 * Domain object representing a detected crash event.
 * Contains all metadata about a crash occurrence including device info,
 * crash classification, and storage references.
 */
public record CrashEvent(
    String id,
    Instant timestamp,
    String deviceSerial,
    String deviceName,
    String packageName,
    String crashType,
    SeverityLevel severity,
    String stackTraceSnippet,
    String crashLogPath,
    Long projectId,
    Long tenantId
) {}
