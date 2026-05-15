package androidtoolkit.backend.entity;

import androidtoolkit.domain.tenant.TenantRole;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a pending invitation for a user to join a tenant.
 */
@Entity
@Table(name = "pending_invites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "email"}))
public class PendingInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantRole role = TenantRole.USER;

    @Column(nullable = false, unique = true)
    private String inviteToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean accepted = false;

    public PendingInvite() {}

    public PendingInvite(Tenant tenant, String email, TenantRole role, String inviteToken, User invitedBy, Instant expiresAt) {
        this.tenant = tenant;
        this.email = email;
        this.role = role;
        this.inviteToken = inviteToken;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getEmail() { return email; }
    public TenantRole getRole() { return role; }
    public String getInviteToken() { return inviteToken; }
    public User getInvitedBy() { return invitedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}
