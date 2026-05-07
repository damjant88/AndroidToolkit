package androidtoolkit.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pending_invites", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "invited_email"}))
public class PendingInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public PendingInvite() {}

    public PendingInvite(User owner, String invitedEmail) {
        this.owner = owner;
        this.invitedEmail = invitedEmail;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public String getInvitedEmail() { return invitedEmail; }
    public Instant getCreatedAt() { return createdAt; }
}
