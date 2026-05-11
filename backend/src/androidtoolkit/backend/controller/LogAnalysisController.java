package androidtoolkit.backend.controller;

import androidtoolkit.backend.dto.AnalysisJobExecutionResponse;
import androidtoolkit.backend.dto.AnalysisReportResponse;
import androidtoolkit.backend.dto.ErrorGroupDto;
import androidtoolkit.backend.dto.ImprovementDto;
import androidtoolkit.backend.dto.LogStructureDto;
import androidtoolkit.backend.dto.PatternDto;
import androidtoolkit.backend.dto.RiskAssessmentDto;
import androidtoolkit.backend.entity.AnalysisJobExecution;
import androidtoolkit.backend.entity.AnalysisReport;
import androidtoolkit.backend.repository.AnalysisJobExecutionRepository;
import androidtoolkit.backend.repository.AnalysisReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST endpoints for analysis report retrieval and job monitoring.
 */
@RestController
@RequestMapping("/api/analysis")
public class LogAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(LogAnalysisController.class);

    private final AnalysisReportRepository reportRepository;
    private final AnalysisJobExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    public LogAnalysisController(
            AnalysisReportRepository reportRepository,
            AnalysisJobExecutionRepository executionRepository,
            ObjectMapper objectMapper
    ) {
        this.reportRepository = reportRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/reports")
    public List<AnalysisReportResponse> getReports(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AnalysisReport> reports = reportRepository
                .findByProjectIdAndReportDateBetween(projectId, from, to);
        return reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/reports/latest")
    public AnalysisReportResponse getLatestReport(@RequestParam Long projectId) {
        return reportRepository.findFirstByProjectIdOrderByReportDateDesc(projectId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @GetMapping("/executions")
    public List<AnalysisJobExecutionResponse> getExecutionHistory(
            @RequestParam(defaultValue = "20") int limit) {
        List<AnalysisJobExecution> executions = executionRepository
                .findByOrderByStartedAtDesc(PageRequest.of(0, limit));
        return executions.stream()
                .map(this::mapExecutionToResponse)
                .collect(Collectors.toList());
    }

    private AnalysisReportResponse mapToResponse(AnalysisReport report) {
        LogStructureDto logStructure = parseLogStructure(report.getLogStructure());
        List<ErrorGroupDto> errors = parseErrors(report.getErrorsFound());
        List<ImprovementDto> improvements = parseImprovements(report.getImprovements());
        RiskAssessmentDto riskAssessment = parseRiskAssessment(report.getRiskAssessment());

        return new AnalysisReportResponse(
                report.getId(),
                report.getProject().getId(),
                report.getProject().getName(),
                report.getReportDate(),
                logStructure,
                errors,
                improvements,
                riskAssessment,
                report.getOverallRiskLevel(),
                report.isFlaggedForAttention(),
                report.getArchivesProcessed(),
                report.getDevicesAnalyzed(),
                report.getCreatedAt()
        );
    }

    private AnalysisJobExecutionResponse mapExecutionToResponse(AnalysisJobExecution execution) {
        return new AnalysisJobExecutionResponse(
                execution.getId(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getStatus(),
                execution.getArchivesProcessed(),
                execution.getErrorDetails()
        );
    }

    private LogStructureDto parseLogStructure(String json) {
        if (json == null || json.isBlank()) return new LogStructureDto(List.of(), List.of());
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            List<Map<String, Object>> patternsRaw = (List<Map<String, Object>>) map.getOrDefault("patterns", List.of());
            List<PatternDto> patterns = patternsRaw.stream()
                    .map(p -> new PatternDto(
                            (String) p.get("name"),
                            (String) p.get("timestampFormat"),
                            (String) p.get("logLevel"),
                            (String) p.get("component"),
                            (String) p.get("messageStructure"),
                            p.get("occurrenceCount") != null ? ((Number) p.get("occurrenceCount")).intValue() : 0
                    ))
                    .collect(Collectors.toList());
            List<String> newPatterns = (List<String>) map.getOrDefault("newPatterns", List.of());
            return new LogStructureDto(patterns, newPatterns);
        } catch (Exception e) {
            log.warn("Failed to parse log structure JSON", e);
            return new LogStructureDto(List.of(), List.of());
        }
    }

    private List<ErrorGroupDto> parseErrors(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list.stream()
                    .map(e -> new ErrorGroupDto(
                            (String) e.get("rootCause"),
                            (String) e.get("component"),
                            (String) e.get("description"),
                            e.get("frequency") != null ? ((Number) e.get("frequency")).intValue() : 0,
                            (List<String>) e.getOrDefault("affectedDevices", List.of()),
                            Boolean.TRUE.equals(e.get("widespread"))
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse errors JSON", e);
            return List.of();
        }
    }

    private List<ImprovementDto> parseImprovements(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list.stream()
                    .map(i -> new ImprovementDto(
                            (String) i.get("priority"),
                            (String) i.get("category"),
                            (String) i.get("recommendation"),
                            (String) i.get("evidence")
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse improvements JSON", e);
            return List.of();
        }
    }

    private RiskAssessmentDto parseRiskAssessment(String json) {
        if (json == null || json.isBlank()) return new RiskAssessmentDto("LOW", List.of(), List.of());
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new RiskAssessmentDto(
                    (String) map.get("riskLevel"),
                    (List<String>) map.getOrDefault("contributingFactors", List.of()),
                    (List<String>) map.getOrDefault("recommendedActions", List.of())
            );
        } catch (Exception e) {
            log.warn("Failed to parse risk assessment JSON", e);
            return new RiskAssessmentDto("LOW", List.of(), List.of());
        }
    }
}
