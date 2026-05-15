package androidtoolkit.backend.security;

import androidtoolkit.backend.service.AuditLogService;
import androidtoolkit.domain.tenant.AuditAction;
import androidtoolkit.domain.tenant.AuditOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Validates that a resource belongs to the current tenant context.
 * Returns HTTP 403 and logs to the audit log when cross-tenant access is detected.
 */
@Service
public class TenantAccessGuard {

    private final AuditLogService auditLogService;

    public TenantAccessGuard(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Validates that the resource's tenant matches the current TenantContext.
     *
     * @param resourceTenantId the tenant_id of the resource being accessed
     * @param resourceDescription a description of the resource for audit logging
     * @throws ResponseStatusException with 403 if tenant mismatch
     */
    public void validateAccess(Long resourceTenantId, String resourceDescription) {
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            // No tenant context set — standalone mode or unauthenticated
            return;
        }
        if (!currentTenantId.equals(resourceTenantId)) {
            auditLogService.log(
                    AuditAction.CROSS_TENANT_ACCESS_ATTEMPT,
                    null, // actor resolved from security context upstream
                    currentTenantId,
                    resourceDescription,
                    AuditOutcome.FAILURE,
                    Map.of("targetTenantId", String.valueOf(resourceTenantId))
            );
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    /**
     * Validates that the resource's tenant matches the current TenantContext.
     *
     * @param resourceTenantId the tenant_id of the resource being accessed
     * @param actorUserId the user attempting access
     * @param resourceDescription a description of the resource for audit logging
     * @throws ResponseStatusException with 403 if tenant mismatch
     */
    public void validateAccess(Long resourceTenantId, Long actorUserId, String resourceDescription) {
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            return;
        }
        if (!currentTenantId.equals(resourceTenantId)) {
            auditLogService.log(
                    AuditAction.CROSS_TENANT_ACCESS_ATTEMPT,
                    actorUserId,
                    currentTenantId,
                    resourceDescription,
                    AuditOutcome.FAILURE,
                    Map.of("targetTenantId", String.valueOf(resourceTenantId))
            );
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}
