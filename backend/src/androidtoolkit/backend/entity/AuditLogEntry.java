package androidtoolkit.backend.entity;

import androidtoolkit.domain.tenant.AuditAction;
import androidtoolkit.domain.tenant.AuditOutcome;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Append-only audit trail entry for ENTERPRISE tenants.
 */
@Entity
@Table(name = "audit_log_entries", indexes = {
        @Index(name = "idx_audit_tenant_timestamp", columnList = "tenant_id, timestamp"),
        @Index(name = "idx_audit_actor", columnList = "actor_user_id")
})
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction actionType;

    @Column(length = 500)
    private String targetResource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditOutcome outcome;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public AuditLogEntry() {}

    public AuditLogEntry(Long tenantId, Long actorUserId, AuditAction actionType,
                         String targetResource, AuditOutcome outcome, String metadata) {
        this.tenantId = tenantId;
        this.actorUserId = actorUserId;
        this.actionType = actionType;
        this.targetResource = targetResource;
        this.outcome = outcome;
        this.metadata = metadata;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getActorUserId() { return actorUserId; }
    public Instant getTimestamp() { return timestamp; }
    public AuditAction getActionType() { return actionType; }
    public String getTargetResource() { return targetResource; }
    public AuditOutcome getOutcome() { return outcome; }
    public String getMetadata() { return metadata; }
}
