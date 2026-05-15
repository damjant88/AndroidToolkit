package androidtoolkit.backend.security;

/**
 * Holds the current tenant identity in a ThreadLocal, set during request processing
 * after JWT authentication. All data access operations use this to scope queries.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    public static Long getTenantId() {
        return currentTenantId.get();
    }

    public static void clear() {
        currentTenantId.remove();
    }
}
