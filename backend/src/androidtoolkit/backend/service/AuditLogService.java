package androidtoolkit.backend.service;

import androidtoolkit.domain.tenant.AuditAction;
import androidtoolkit.domain.tenant.AuditOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service for recording and querying audit log entries.
 * Only records entries for ENTERPRISE tier tenants.
 */
public interface AuditLogService {

    /**
     * Record an audit log entry. Only persists if the tenant's tier is ENTERPRISE.
     *
     * @param action         the action being audited
     * @param actorUserId    the user performing the action
     * @param tenantId       the tenant context
     * @param targetResource the resource being acted upon
     * @param outcome        whether the action succeeded or failed
     * @param metadata       additional key-value metadata about the action
     */
    void log(AuditAction action, Long actorUserId, Long tenantId,
             String targetResource, AuditOutcome outcome, Map<String, String> metadata);

    /**
     * Query audit log entries for a tenant with filtering and pagination.
     *
     * @param tenantId the tenant to query
     * @param filter   the filter criteria
     * @param pageable pagination parameters
     * @return a page of matching audit log entries
     */
    Page<AuditLogEntry> query(Long tenantId, AuditLogFilter filter, Pageable pageable);
}
