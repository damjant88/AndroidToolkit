package androidtoolkit.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Extracts the tenant_id from the authenticated user's JWT claims
 * and sets it in the TenantContext ThreadLocal for the duration of the request.
 */
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    public TenantContextInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var claims = jwtService.parseToken(token);
                Object tenantIdClaim = claims.get("tenantId");
                if (tenantIdClaim != null) {
                    Long tenantId = ((Number) tenantIdClaim).longValue();
                    TenantContext.setTenantId(tenantId);
                }
            } catch (Exception ignored) {
                // If token parsing fails, tenant context remains unset.
                // The security filter will handle authentication rejection.
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}
