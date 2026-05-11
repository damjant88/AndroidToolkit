package androidtoolkit.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "analysis_reports")
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(columnDefinition = "TEXT")
    private String logStructure;

    @Column(columnDefinition = "TEXT")
    private String errorsFound;

    @Column(columnDefinition = "TEXT")
    private String improvements;

    @Column(columnDefinition = "TEXT")
    private String riskAssessment;

    @Column(nullable = false)
    private String overallRiskLevel;

    @Column(nullable = false)
    private boolean flaggedForAttention;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private int archivesProcessed;

    @Column(nullable = false)
    private int devicesAnalyzed;

    public AnalysisReport() {}

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public String getLogStructure() { return logStructure; }
    public void setLogStructure(String logStructure) { this.logStructure = logStructure; }

    public String getErrorsFound() { return errorsFound; }
    public void setErrorsFound(String errorsFound) { this.errorsFound = errorsFound; }

    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }

    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }

    public String getOverallRiskLevel() { return overallRiskLevel; }
    public void setOverallRiskLevel(String overallRiskLevel) { this.overallRiskLevel = overallRiskLevel; }

    public boolean isFlaggedForAttention() { return flaggedForAttention; }
    public void setFlaggedForAttention(boolean flaggedForAttention) { this.flaggedForAttention = flaggedForAttention; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getArchivesProcessed() { return archivesProcessed; }
    public void setArchivesProcessed(int archivesProcessed) { this.archivesProcessed = archivesProcessed; }

    public int getDevicesAnalyzed() { return devicesAnalyzed; }
    public void setDevicesAnalyzed(int devicesAnalyzed) { this.devicesAnalyzed = devicesAnalyzed; }
}
