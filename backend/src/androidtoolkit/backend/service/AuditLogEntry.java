package androidtoolkit.backend.service;

import androidtoolkit.domain.tenant.AuditAction;
import androidtoolkit.domain.tenant.AuditOutcome;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single audit log entry returned from queries.
 *
 * @param id             the entry identifier
 * @param tenantId       the tenant this entry belongs to
 * @param actorUserId    the user who performed the action
 * @param timestamp      when the action occurred
 * @param action         the type of action
 * @param targetResource the resource acted upon
 * @param outcome        success or failure
 * @param metadata       additional context about the action
 */
public record AuditLogEntry(
        Long id,
        Long tenantId,
        Long actorUserId,
        Instant timestamp,
        AuditAction action,
        String targetResource,
        AuditOutcome outcome,
        Map<String, String> metadata
) {}
