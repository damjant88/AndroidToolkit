package androidtoolkit.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AnalysisReportResponse(
    Long id,
    Long projectId,
    String projectName,
    LocalDate reportDate,
    LogStructureDto logStructure,
    List<ErrorGroupDto> errorsFound,
    List<ImprovementDto> improvements,
    RiskAssessmentDto riskAssessment,
    String overallRiskLevel,
    boolean flaggedForAttention,
    int archivesProcessed,
    int devicesAnalyzed,
    Instant createdAt
) {}
