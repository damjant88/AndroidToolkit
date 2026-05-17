package androidtoolkit.backend.crash;

import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.Tenant;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;

/**
 * JPA entity representing a persisted crash event.
 * Contains all metadata about a crash occurrence including device info,
 * crash classification, storage references, and acknowledgement state.
 */
@Entity
@Table(name = "crash_events", indexes = {
    @Index(name = "idx_crash_project_timestamp", columnList = "project_id, timestamp DESC"),
    @Index(name = "idx_crash_device_timestamp", columnList = "device_serial, timestamp DESC"),
    @Index(name = "idx_crash_tenant", columnList = "tenant_id")
})
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CrashEventEntity {

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
    private Instant timestamp;

    @Column(name = "device_serial", nullable = false)
    private String deviceSerial;

    @Column
    private String deviceName;

    @Column
    private String packageName;

    @Column(nullable = false)
    private String crashType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SeverityLevel severity;

    @Column(columnDefinition = "TEXT")
    private String stackTraceSnippet;

    @Column
    private String crashLogPath;

    @Column
    private String autoPullLogPath;

    @Column(nullable = false)
    private boolean acknowledged = false;

    @Column
    private Instant acknowledgedAt;

    public CrashEventEntity() {}

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getDeviceSerial() { return deviceSerial; }
    public void setDeviceSerial(String deviceSerial) { this.deviceSerial = deviceSerial; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getCrashType() { return crashType; }
    public void setCrashType(String crashType) { this.crashType = crashType; }

    public SeverityLevel getSeverity() { return severity; }
    public void setSeverity(SeverityLevel severity) { this.severity = severity; }

    public String getStackTraceSnippet() { return stackTraceSnippet; }
    public void setStackTraceSnippet(String stackTraceSnippet) { this.stackTraceSnippet = stackTraceSnippet; }

    public String getCrashLogPath() { return crashLogPath; }
    public void setCrashLogPath(String crashLogPath) { this.crashLogPath = crashLogPath; }

    public String getAutoPullLogPath() { return autoPullLogPath; }
    public void setAutoPullLogPath(String autoPullLogPath) { this.autoPullLogPath = autoPullLogPath; }

    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
