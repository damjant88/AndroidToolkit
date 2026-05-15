package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.PendingInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingInviteRepository extends JpaRepository<PendingInvite, Long> {
    Optional<PendingInvite> findByInviteToken(String inviteToken);
    List<PendingInvite> findByTenantId(Long tenantId);
    List<PendingInvite> findByEmail(String email);
    boolean existsByTenantIdAndEmail(Long tenantId, String email);
    long countByTenantIdAndAcceptedTrue(Long tenantId);
}
