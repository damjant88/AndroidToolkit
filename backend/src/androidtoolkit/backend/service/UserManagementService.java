package androidtoolkit.backend.service;

import androidtoolkit.backend.entity.PendingInvite;
import androidtoolkit.backend.entity.Tenant;
import androidtoolkit.backend.entity.TenantMembership;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.PendingInviteRepository;
import androidtoolkit.backend.repository.TenantMembershipRepository;
import androidtoolkit.backend.repository.TenantRepository;
import androidtoolkit.backend.repository.UserRepository;
import androidtoolkit.domain.tenant.AuditAction;
import androidtoolkit.domain.tenant.AuditOutcome;
import androidtoolkit.domain.tenant.TenantRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserManagementService {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final PendingInviteRepository pendingInviteRepository;
    private final UserRepository userRepository;
    private final TierEnforcer tierEnforcer;
    private final AuditLogService auditLogService;

    public UserManagementService(TenantRepository tenantRepository,
                                 TenantMembershipRepository membershipRepository,
                                 PendingInviteRepository pendingInviteRepository,
                                 UserRepository userRepository,
                                 TierEnforcer tierEnforcer,
                                 AuditLogService auditLogService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.pendingInviteRepository = pendingInviteRepository;
        this.userRepository = userRepository;
        this.tierEnforcer = tierEnforcer;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PendingInvite inviteUser(Long tenantId, String email, TenantRole role, User invitedBy) {
        tierEnforcer.checkUserLimit(tenantId);

        if (pendingInviteRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already sent to " + email);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        PendingInvite invite = new PendingInvite(
                tenant, email, role,
                UUID.randomUUID().toString(),
                invitedBy,
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        pendingInviteRepository.save(invite);

        auditLogService.log(AuditAction.USER_INVITED, invitedBy.getId(), tenantId,
                "user:" + email, AuditOutcome.SUCCESS, Map.of("role", role.name()));

        return invite;
    }

    @Transactional
    public User acceptInvitation(String inviteToken) {
        PendingInvite invite = pendingInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invitation token"));

        if (invite.isAccepted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already accepted");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invitation has expired");
        }

        // Find or create user
        User user = userRepository.findByEmail(invite.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User must register first before accepting invitation"));

        // Create membership
        TenantMembership membership = new TenantMembership(invite.getTenant(), user, invite.getRole());
        membershipRepository.save(membership);

        invite.setAccepted(true);
        pendingInviteRepository.save(invite);

        return user;
    }

    @Transactional
    public void assignRole(Long tenantId, Long userId, TenantRole newRole, Long actorUserId) {
        TenantMembership membership = membershipRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a member of this tenant"));

        TenantRole oldRole = membership.getRole();
        membership.setRole(newRole);
        membershipRepository.save(membership);

        auditLogService.log(AuditAction.ROLE_ASSIGNED, actorUserId, tenantId,
                "user:" + userId, AuditOutcome.SUCCESS,
                Map.of("oldRole", oldRole.name(), "newRole", newRole.name()));
    }

    public List<TenantMembership> listMembers(Long tenantId) {
        return membershipRepository.findByTenantId(tenantId);
    }
}
