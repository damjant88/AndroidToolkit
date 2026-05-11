package androidtoolkit.backend.job;

import androidtoolkit.backend.entity.AnalysisJobExecution;
import androidtoolkit.backend.entity.AnalysisReport;
import androidtoolkit.backend.entity.LogUploadMetadata;
import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.repository.AnalysisJobExecutionRepository;
import androidtoolkit.backend.repository.AnalysisReportRepository;
import androidtoolkit.backend.repository.LogUploadMetadataRepository;
import androidtoolkit.backend.service.AiAnalysisEngine;
import androidtoolkit.backend.service.AnalysisResult;
import androidtoolkit.backend.service.SharedStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Scheduled overnight job that processes collected log archives using AI analysis.
 * Runs daily at a configurable time (default 02:00 AM).
 */
@Component
public class LogAnalysisJob {

    private static final Logger log = LoggerFactory.getLogger(LogAnalysisJob.class);
    private static final int MAX_RETRIES = 2;

    private final LogUploadMetadataRepository metadataRepository;
    private final SharedStorageService sharedStorageService;
    private final AnalysisReportRepository reportRepository;
    private final AnalysisJobExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private AiAnalysisEngine aiAnalysisEngine;

    public LogAnalysisJob(
            LogUploadMetadataRepository metadataRepository,
            SharedStorageService sharedStorageService,
            AnalysisReportRepository reportRepository,
            AnalysisJobExecutionRepository executionRepository,
            ObjectMapper objectMapper
    ) {
        this.metadataRepository = metadataRepository;
        this.sharedStorageService = sharedStorageService;
        this.reportRepository = reportRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "${log.analysis.cron:0 0 2 * * *}")
    public void executeAnalysis() {
        // Skip if previous job is still running
        List<AnalysisJobExecution> recentJobs = executionRepository
                .findByOrderByStartedAtDesc(PageRequest.of(0, 1));
        if (!recentJobs.isEmpty() && "RUNNING".equals(recentJobs.get(0).getStatus())) {
            log.warn("Previous analysis job still running, skipping this execution");
            return;
        }

        if (aiAnalysisEngine == null) {
            log.info("No AiAnalysisEngine implementation available, skipping analysis");
            return;
        }

        // Record job start
        AnalysisJobExecution execution = new AnalysisJobExecution();
        execution.setStartedAt(Instant.now());
        execution.setStatus("RUNNING");
        execution = executionRepository.save(execution);

        try {
            // Query metadata from past 24 hours
            Instant now = Instant.now();
            Instant twentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS);
            List<LogUploadMetadata> recentUploads = metadataRepository
                    .findByUploadedAtBetween(twentyFourHoursAgo, now);

            if (recentUploads.isEmpty()) {
                log.info("No log uploads in the past 24 hours, nothing to analyze");
                execution.setStatus("COMPLETED");
                execution.setCompletedAt(Instant.now());
                execution.setArchivesProcessed(0);
                executionRepository.save(execution);
                return;
            }

            // Group by project
            Map<Project, List<LogUploadMetadata>> byProject = recentUploads.stream()
                    .filter(m -> m.getProject() != null)
                    .collect(Collectors.groupingBy(LogUploadMetadata::getProject));

            int totalArchivesProcessed = 0;
            List<String> errors = new ArrayList<>();

            for (Map.Entry<Project, List<LogUploadMetadata>> entry : byProject.entrySet()) {
                Project project = entry.getKey();
                List<LogUploadMetadata> projectMetadata = entry.getValue();

                boolean success = processProjectWithRetry(project, projectMetadata);
                if (success) {
                    totalArchivesProcessed += projectMetadata.size();
                } else {
                    errors.add("Failed to analyze project: " + project.getName());
                }
            }

            // Update execution status
            execution.setArchivesProcessed(totalArchivesProcessed);
            execution.setCompletedAt(Instant.now());
            if (errors.isEmpty()) {
                execution.setStatus("COMPLETED");
            } else {
                execution.setStatus("COMPLETED");
                execution.setErrorDetails(String.join("; ", errors));
            }
            executionRepository.save(execution);

            log.info("Analysis job completed. Processed {} archives across {} projects",
                    totalArchivesProcessed, byProject.size());

        } catch (Exception e) {
            log.error("Analysis job failed with unrecoverable error", e);
            execution.setStatus("FAILED");
            execution.setCompletedAt(Instant.now());
            execution.setErrorDetails(e.getMessage());
            executionRepository.save(execution);
        }
    }

    private boolean processProjectWithRetry(Project project, List<LogUploadMetadata> metadata) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                processProject(project, metadata);
                return true;
            } catch (Exception e) {
                log.warn("Attempt {}/{} failed for project '{}': {}",
                        attempt, MAX_RETRIES, project.getName(), e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("All retries exhausted for project '{}'", project.getName(), e);
                }
            }
        }
        return false;
    }

    private void processProject(Project project, List<LogUploadMetadata> metadata) throws IOException {
        // Read log content from archives grouped by device
        Map<String, List<String>> logContentByDevice = new HashMap<>();

        for (LogUploadMetadata meta : metadata) {
            Path archivePath = Path.of(meta.getFilePath());
            if (!Files.exists(archivePath)) {
                log.warn("Archive not found: {}", meta.getFilePath());
                continue;
            }

            List<String> logContents = extractLogContent(archivePath);
            logContentByDevice.computeIfAbsent(meta.getDeviceSerial(), k -> new ArrayList<>())
                    .addAll(logContents);
        }

        if (logContentByDevice.isEmpty()) {
            log.info("No log content extracted for project '{}'", project.getName());
            return;
        }

        // Call AI analysis engine
        AnalysisResult result = aiAnalysisEngine.analyze(project.getName(), logContentByDevice);

        // Detect new patterns by comparing with previous report
        List<String> newPatterns = detectNewPatterns(project, result);

        // Map result to entity and persist
        AnalysisReport report = mapToReport(project, result, metadata, logContentByDevice, newPatterns);
        reportRepository.save(report);

        log.info("Analysis report saved for project '{}': risk={}, flagged={}",
                project.getName(), report.getOverallRiskLevel(), report.isFlaggedForAttention());
    }

    private List<String> extractLogContent(Path zipPath) throws IOException {
        List<String> contents = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    contents.add(sb.toString());
                }
                zis.closeEntry();
            }
        }
        return contents;
    }

    private List<String> detectNewPatterns(Project project, AnalysisResult result) {
        Optional<AnalysisReport> previousReport = reportRepository
                .findFirstByProjectIdOrderByReportDateDesc(project.getId());

        if (previousReport.isEmpty() || result.logStructure() == null) {
            // All patterns are new if no previous report
            if (result.logStructure() != null) {
                return result.logStructure().patterns().stream()
                        .map(AnalysisResult.Pattern::name)
                        .collect(Collectors.toList());
            }
            return List.of();
        }

        // Parse previous patterns from stored JSON
        try {
            String prevLogStructure = previousReport.get().getLogStructure();
            if (prevLogStructure == null || prevLogStructure.isBlank()) {
                return result.logStructure().patterns().stream()
                        .map(AnalysisResult.Pattern::name)
                        .collect(Collectors.toList());
            }

            // Extract pattern names from previous report's JSON
            Map<String, Object> prevStructure = objectMapper.readValue(prevLogStructure, Map.class);
            List<Map<String, Object>> prevPatterns = (List<Map<String, Object>>) prevStructure.getOrDefault("patterns", List.of());
            List<String> prevPatternNames = prevPatterns.stream()
                    .map(p -> (String) p.get("name"))
                    .collect(Collectors.toList());

            // New patterns = current - previous
            return result.logStructure().patterns().stream()
                    .map(AnalysisResult.Pattern::name)
                    .filter(name -> !prevPatternNames.contains(name))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse previous report patterns for project '{}': {}",
                    project.getName(), e.getMessage());
            return List.of();
        }
    }

    private AnalysisReport mapToReport(Project project, AnalysisResult result,
                                        List<LogUploadMetadata> metadata,
                                        Map<String, List<String>> logContentByDevice,
                                        List<String> newPatterns) {
        AnalysisReport report = new AnalysisReport();
        report.setProject(project);
        report.setReportDate(LocalDate.now());
        report.setCreatedAt(Instant.now());
        report.setArchivesProcessed(metadata.size());
        report.setDevicesAnalyzed(logContentByDevice.size());

        // Serialize sections to JSON
        try {
            // Log structure with new patterns
            Map<String, Object> logStructureMap = new HashMap<>();
            if (result.logStructure() != null) {
                logStructureMap.put("patterns", result.logStructure().patterns());
            }
            logStructureMap.put("newPatterns", newPatterns);
            report.setLogStructure(objectMapper.writeValueAsString(logStructureMap));

            report.setErrorsFound(objectMapper.writeValueAsString(result.errorsFound()));
            report.setImprovements(objectMapper.writeValueAsString(result.improvements()));
            report.setRiskAssessment(objectMapper.writeValueAsString(result.riskAssessment()));
        } catch (Exception e) {
            log.error("Failed to serialize analysis result to JSON", e);
            report.setLogStructure("{}");
            report.setErrorsFound("[]");
            report.setImprovements("[]");
            report.setRiskAssessment("{}");
        }

        // Risk level and attention flagging
        String riskLevel = result.overallRiskLevel() != null ? result.overallRiskLevel() : "LOW";
        report.setOverallRiskLevel(riskLevel);
        report.setFlaggedForAttention("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel));

        return report;
    }
}
