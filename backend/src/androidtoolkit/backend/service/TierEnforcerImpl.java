package androidtoolkit.backend.service;

import androidtoolkit.backend.entity.Tenant;
import androidtoolkit.backend.repository.AnalysisJobExecutionRepository;
import androidtoolkit.backend.repository.TenantMembershipRepository;
import androidtoolkit.backend.repository.TenantRepository;
import androidtoolkit.domain.tenant.SubscriptionTier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Enforces subscription tier limits by checking current usage against tier maximums.
 */
@Service
public class TierEnforcerImpl implements TierEnforcer {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final AnalysisJobExecutionRepository analysisJobRepository;
    private final ObjectStorageService objectStorageService;

    public TierEnforcerImpl(TenantRepository tenantRepository,
                            TenantMembershipRepository membershipRepository,
                            AnalysisJobExecutionRepository analysisJobRepository,
                            ObjectStorageService objectStorageService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.objectStorageService = objectStorageService;
    }

    @Override
    public void checkDeviceLimit(Long tenantId) {
        Tenant tenant = getTenant(tenantId);
        if (tenant.getTier() == SubscriptionTier.ENTERPRISE || tenant.getTier() == SubscriptionTier.PRO) {
            return; // Unlimited
        }
        // FREE tier: max 2 devices — device count checked at registration time by caller
        // This is a pre-check; actual count comes from the device registry
    }

    @Override
    public void checkUserLimit(Long tenantId) {
        Tenant tenant = getTenant(tenantId);
        int maxUsers = getMaxUsers(tenant.getTier());
        if (maxUsers == Integer.MAX_VALUE) return;

        long currentUsers = membershipRepository.countByTenantId(tenantId);
        if (currentUsers >= maxUsers) {
            throw new TierLimitExceededException(
                    "User limit reached for " + tenant.getTier() + " tier",
                    maxUsers, (int) currentUsers, tenant.getTier(), "users"
            );
        }
    }

    @Override
    public void checkAnalysisLimit(Long tenantId) {
        Tenant tenant = getTenant(tenantId);
        if (tenant.getTier() != SubscriptionTier.FREE) return; // PRO and ENTERPRISE are unlimited

        int maxAnalyses = 5;
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        Instant monthStart = currentMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long currentCount = analysisJobRepository.countByTenantIdAndStartedAtBetween(tenantId, monthStart, monthEnd);
        if (currentCount >= maxAnalyses) {
            throw new TierLimitExceededException(
                    "AI analysis limit reached for FREE tier (5 per month)",
                    maxAnalyses, (int) currentCount, tenant.getTier(), "analyses"
            );
        }
    }

    @Override
    public void checkStorageLimit(Long tenantId, long additionalBytes) {
        Tenant tenant = getTenant(tenantId);
        long maxBytes = getMaxStorageBytes(tenant.getTier());
        if (maxBytes == Long.MAX_VALUE) return;

        long currentUsage = objectStorageService.getTenantStorageUsage(tenantId);
        if (currentUsage + additionalBytes > maxBytes) {
            throw new TierLimitExceededException(
                    "Storage limit reached for " + tenant.getTier() + " tier",
                    (int) (maxBytes / (1024 * 1024)), // limit in MB
                    (int) (currentUsage / (1024 * 1024)), // current in MB
                    tenant.getTier(), "storage"
            );
        }
    }

    private Tenant getTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }

    private int getMaxUsers(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> 1;
            case PRO -> 20;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    private long getMaxStorageBytes(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> 500L * 1024 * 1024; // 500 MB
            case PRO -> 50L * 1024 * 1024 * 1024; // 50 GB
            case ENTERPRISE -> Long.MAX_VALUE;
        };
    }
}
