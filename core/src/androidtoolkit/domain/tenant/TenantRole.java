package androidtoolkit.domain.tenant;

/**
 * Roles that a user can hold within a tenant.
 */
public enum TenantRole {

    /**
     * Tenant owner — full control including billing and tenant deletion.
     */
    OWNER,

    /**
     * Administrator — can manage users, projects, and roles.
     */
    ADMIN,

    /**
     * Regular user — can view projects and use devices within the tenant.
     */
    USER
}
