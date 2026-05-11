package androidtoolkit.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "analysis_job_executions")
public class AnalysisJobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private String status; // RUNNING, COMPLETED, FAILED

    private int archivesProcessed;

    @Column(columnDefinition = "TEXT")
    private String errorDetails;

    public AnalysisJobExecution() {}

    public Long getId() { return id; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getArchivesProcessed() { return archivesProcessed; }
    public void setArchivesProcessed(int archivesProcessed) { this.archivesProcessed = archivesProcessed; }

    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
}
