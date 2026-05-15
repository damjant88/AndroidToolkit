package androidtoolkit.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "log_upload_metadata")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class LogUploadMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String deviceSerial;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false)
    private LocalDate logDate;

    public LogUploadMetadata() {}

    public LogUploadMetadata(Project project, String deviceSerial, Instant uploadedAt,
                             String filePath, Long fileSizeBytes, LocalDate logDate) {
        this.project = project;
        this.deviceSerial = deviceSerial;
        this.uploadedAt = uploadedAt;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.logDate = logDate;
    }

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getDeviceSerial() { return deviceSerial; }
    public void setDeviceSerial(String deviceSerial) { this.deviceSerial = deviceSerial; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
}
