package androidtoolkit.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "access_grants", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "granted_user_id"}))
public class AccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_user_id", nullable = false)
    private User grantedUser;

    @Column(nullable = false)
    private boolean permanent = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public AccessGrant() {}

    public AccessGrant(User owner, User grantedUser, boolean permanent) {
        this.owner = owner;
        this.grantedUser = grantedUser;
        this.permanent = permanent;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public User getGrantedUser() { return grantedUser; }
    public boolean isPermanent() { return permanent; }
    public void setPermanent(boolean permanent) { this.permanent = permanent; }
    public Instant getCreatedAt() { return createdAt; }
}
