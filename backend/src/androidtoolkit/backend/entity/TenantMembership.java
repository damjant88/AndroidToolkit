package androidtoolkit.backend.entity;

import androidtoolkit.domain.tenant.TenantRole;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Maps a user to a tenant with a specific role.
 * A user can belong to multiple tenants with different roles.
 */
@Entity
@Table(name = "tenant_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"}))
public class TenantMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantRole role;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    public TenantMembership() {}

    public TenantMembership(Tenant tenant, User user, TenantRole role) {
        this.tenant = tenant;
        this.user = user;
        this.role = role;
    }

    public Long getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public User getUser() { return user; }
    public TenantRole getRole() { return role; }
    public void setRole(TenantRole role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
}
