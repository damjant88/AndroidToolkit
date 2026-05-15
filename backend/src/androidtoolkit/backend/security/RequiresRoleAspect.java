package androidtoolkit.backend.security;

import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.TenantMembershipRepository;
import androidtoolkit.domain.tenant.TenantRole;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * AOP aspect that enforces the @RequiresRole annotation by checking
 * the authenticated user's role within the current tenant.
 */
@Aspect
@Component
public class RequiresRoleAspect {

    private final TenantMembershipRepository membershipRepository;

    public RequiresRoleAspect(TenantMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // Standalone mode — no tenant context, allow access
            return joinPoint.proceed();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        var membership = membershipRepository.findByTenantIdAndUserId(tenantId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this tenant"));

        TenantRole requiredRole = requiresRole.value();
        if (!hasMinimumRole(membership.getRole(), requiredRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Requires " + requiredRole + " role or higher");
        }

        return joinPoint.proceed();
    }

    /**
     * Check if actualRole meets or exceeds the required role.
     * Hierarchy: OWNER > ADMIN > USER
     */
    private boolean hasMinimumRole(TenantRole actualRole, TenantRole requiredRole) {
        return actualRole.ordinal() <= requiredRole.ordinal();
    }
}
