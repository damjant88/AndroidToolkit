package androidtoolkit.backend.service;

import java.util.List;

/**
 * Structured result of AI-driven log analysis for a single project.
 * Contains all report sections: log structure patterns, errors found,
 * improvement suggestions, and risk assessment.
 */
public record AnalysisResult(
    LogStructure logStructure,
    List<ErrorGroup> errorsFound,
    List<Improvement> improvements,
    RiskAssessment riskAssessment,
    String overallRiskLevel
) {

    /**
     * Identified log structure patterns from the analyzed logs.
     */
    public record LogStructure(
        List<Pattern> patterns
    ) {}

    /**
     * A single recurring log pattern identified during analysis.
     */
    public record Pattern(
        String name,
        String timestampFormat,
        String logLevel,
        String component,
        String messageStructure,
        int occurrenceCount
    ) {}

    /**
     * A group of related errors identified by root cause or component.
     */
    public record ErrorGroup(
        String rootCause,
        String component,
        String description,
        int frequency,
        List<String> affectedDevices,
        boolean widespread
    ) {}

    /**
     * A prioritized improvement suggestion based on detected log patterns.
     */
    public record Improvement(
        String priority,
        String category,
        String recommendation,
        String evidence
    ) {}

    /**
     * Risk assessment for the analyzed project logs.
     */
    public record RiskAssessment(
        String riskLevel,
        List<String> contributingFactors,
        List<String> recommendedActions
    ) {}
}
