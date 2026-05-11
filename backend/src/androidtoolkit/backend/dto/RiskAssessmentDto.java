package androidtoolkit.backend.dto;

import java.util.List;

public record RiskAssessmentDto(
    String riskLevel,
    List<String> contributingFactors,
    List<String> recommendedActions
) {}
