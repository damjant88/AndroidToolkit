package androidtoolkit.backend.service;

import androidtoolkit.domain.tenant.AuditAction;

import java.time.Instant;

/**
 * Filter criteria for querying audit log entries.
 *
 * @param dateFrom   start of the date range (inclusive), or null for no lower bound
 * @param dateTo     end of the date range (inclusive), or null for no upper bound
 * @param actorUserId filter by the user who performed the action, or null for all users
 * @param actionType filter by action type, or null for all action types
 */
public record AuditLogFilter(
        Instant dateFrom,
        Instant dateTo,
        Long actorUserId,
        AuditAction actionType
) {
    /**
     * Creates an empty filter that matches all entries.
     */
    public static AuditLogFilter all() {
        return new AuditLogFilter(null, null, null, null);
    }
}
