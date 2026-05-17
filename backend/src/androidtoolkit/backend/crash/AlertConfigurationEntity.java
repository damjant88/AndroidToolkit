package androidtoolkit.backend.crash;

import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.Tenant;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

/**
 * JPA entity representing per-project alert configuration settings.
 * Controls crash detection behavior including enabled state, auto-pull,
 * notification preferences, and buffer size.
 */
@Entity
@Table(name = "alert_configurations")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class AlertConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", unique = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false)
    private boolean crashDetectionEnabled = true;

    @Column(nullable = false)
    private boolean autoPullEnabled = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationPreference notificationPreference = NotificationPreference.BROWSER;

    @Column(nullable = false)
    private int bufferSize = 500;

    public AlertConfigurationEntity() {}

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public boolean isCrashDetectionEnabled() { return crashDetectionEnabled; }
    public void setCrashDetectionEnabled(boolean crashDetectionEnabled) { this.crashDetectionEnabled = crashDetectionEnabled; }

    public boolean isAutoPullEnabled() { return autoPullEnabled; }
    public void setAutoPullEnabled(boolean autoPullEnabled) { this.autoPullEnabled = autoPullEnabled; }

    public NotificationPreference getNotificationPreference() { return notificationPreference; }
    public void setNotificationPreference(NotificationPreference notificationPreference) { this.notificationPreference = notificationPreference; }

    public int getBufferSize() { return bufferSize; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
}
