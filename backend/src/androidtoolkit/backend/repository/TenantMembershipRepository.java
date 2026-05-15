package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.TenantMembership;
import androidtoolkit.domain.tenant.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, Long> {

    List<TenantMembership> findByTenantId(Long tenantId);

    Optional<TenantMembership> findByTenantIdAndUserId(Long tenantId, Long userId);

    boolean existsByTenantIdAndUserId(Long tenantId, Long userId);

    long countByTenantId(Long tenantId);

    List<TenantMembership> findByTenantIdAndRole(Long tenantId, TenantRole role);
}
