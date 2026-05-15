package androidtoolkit.backend.service;

import androidtoolkit.domain.tenant.SubscriptionTier;

/**
 * Thrown when a tenant operation would exceed the subscription tier's limits.
 * Maps to HTTP 429 (Too Many Requests) in the global exception handler.
 */
public class TierLimitExceededException extends RuntimeException {

    private final int limit;
    private final int current;
    private final SubscriptionTier tier;
    private final String resource;

    public TierLimitExceededException(String message, int limit, int current, SubscriptionTier tier, String resource) {
        super(message);
        this.limit = limit;
        this.current = current;
        this.tier = tier;
        this.resource = resource;
    }

    public int getLimit() {
        return limit;
    }

    public int getCurrent() {
        return current;
    }

    public SubscriptionTier getTier() {
        return tier;
    }

    public String getResource() {
        return resource;
    }
}
