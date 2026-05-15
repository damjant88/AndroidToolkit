package androidtoolkit.backend.controller;

import androidtoolkit.backend.security.RequiresRole;
import androidtoolkit.backend.security.TenantContext;
import androidtoolkit.backend.service.AuditLogEntry;
import androidtoolkit.backend.service.AuditLogFilter;
import androidtoolkit.backend.service.AuditLogService;
import androidtoolkit.domain.tenant.AuditAction;
import androidtoolkit.domain.tenant.TenantRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @RequiresRole(TenantRole.OWNER)
    public Page<AuditLogEntry> queryAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) AuditAction actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Long tenantId = TenantContext.getTenantId();
        AuditLogFilter filter = new AuditLogFilter(dateFrom, dateTo, actorId, actionType);
        return auditLogService.query(tenantId, filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
    }
}
