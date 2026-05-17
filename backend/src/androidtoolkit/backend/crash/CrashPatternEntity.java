package androidtoolkit.backend.crash;

import androidtoolkit.backend.entity.Project;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity representing a crash detection pattern.
 * Patterns can be default (system-provided) or custom (user-defined per project).
 * Each pattern has a regex string, severity classification, and enabled state.
 */
@Entity
@Table(name = "crash_patterns")
public class CrashPatternEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String regex;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SeverityLevel severity;

    @Column(nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public CrashPatternEntity() {}

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getRegex() { return regex; }
    public void setRegex(String regex) { this.regex = regex; }

    public SeverityLevel getSeverity() { return severity; }
    public void setSeverity(SeverityLevel severity) { this.severity = severity; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
