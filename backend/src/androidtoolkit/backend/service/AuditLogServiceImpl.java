package androidtoolkit.backend.service;

import androidtoolkit.backend.entity.AuditLogEntry;
import androidtoolkit.backend.entity.Tenant;
import androidtoolkit.backend.repository.AuditLogEntryRepository;
import androidtoolkit.backend.repository.TenantRepository;
import androidtoolkit.domain.tenant.AuditAction;
import androidtoolkit.domain.tenant.AuditOutcome;
import androidtoolkit.domain.tenant.SubscriptionTier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogEntryRepository repository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogServiceImpl(AuditLogEntryRepository repository, TenantRepository tenantRepository) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void log(AuditAction action, Long actorUserId, Long tenantId,
                    String targetResource, AuditOutcome outcome, Map<String, String> metadata) {
        if (tenantId == null) return;

        // Only log for ENTERPRISE tenants
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getTier() != SubscriptionTier.ENTERPRISE) {
            return;
        }

        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception ignored) {}
        }

        AuditLogEntry entry = new AuditLogEntry(tenantId, actorUserId, action, targetResource, outcome, metadataJson);
        repository.save(entry);
    }

    @Override
    public Page<androidtoolkit.backend.service.AuditLogEntry> query(Long tenantId, AuditLogFilter filter, Pageable pageable) {
        Page<AuditLogEntry> page = repository.findByFilters(
                tenantId, filter.dateFrom(), filter.dateTo(), filter.actorUserId(), filter.actionType(), pageable);

        return page.map(e -> new androidtoolkit.backend.service.AuditLogEntry(
                e.getId(), e.getTenantId(), e.getActorUserId(), e.getTimestamp(),
                e.getActionType(), e.getTargetResource(), e.getOutcome(), parseMetadata(e.getMetadata())
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
