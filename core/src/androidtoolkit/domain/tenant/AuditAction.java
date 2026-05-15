package androidtoolkit.domain.tenant;

/**
 * Actions that are recorded in the audit log for enterprise tenants.
 */
public enum AuditAction {

    // Authentication events
    LOGIN,
    LOGOUT,
    TOKEN_REFRESH,
    LOGIN_FAILED,

    // Role and permission changes
    ROLE_ASSIGNED,
    ROLE_REVOKED,
    USER_INVITED,
    USER_REMOVED,

    // Data operations
    DATA_EXPORT,
    DATA_IMPORT,
    MIGRATION_STARTED,
    MIGRATION_COMPLETED,

    // Project operations
    PROJECT_CREATED,
    PROJECT_DELETED,
    PROJECT_ARCHIVED,

    // Security events
    CROSS_TENANT_ACCESS_ATTEMPT,
    TIER_LIMIT_EXCEEDED
}
