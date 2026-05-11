package androidtoolkit.backend.dto;

import java.time.Instant;

public record AnalysisJobExecutionResponse(
    Long id,
    Instant startedAt,
    Instant completedAt,
    String status,
    int archivesProcessed,
    String errorDetails
) {}
