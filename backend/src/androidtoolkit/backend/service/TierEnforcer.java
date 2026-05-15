package androidtoolkit.backend.service;

/**
 * Enforces subscription tier limits for tenant operations.
 * Each method checks whether the tenant has capacity for the requested
 * operation and throws {@link TierLimitExceededException} if the limit
 * would be exceeded.
 */
public interface TierEnforcer {

    /**
     * Check if the tenant can register another device.
     *
     * @param tenantId the tenant identifier
     * @throws TierLimitExceededException if the device limit is reached
     */
    void checkDeviceLimit(Long tenantId);

    /**
     * Check if the tenant can add another user.
     *
     * @param tenantId the tenant identifier
     * @throws TierLimitExceededException if the user limit is reached
     */
    void checkUserLimit(Long tenantId);

    /**
     * Check if the tenant can execute another AI analysis this month.
     *
     * @param tenantId the tenant identifier
     * @throws TierLimitExceededException if the monthly analysis limit is reached
     */
    void checkAnalysisLimit(Long tenantId);

    /**
     * Check if the tenant can store additional bytes.
     *
     * @param tenantId        the tenant identifier
     * @param additionalBytes the number of bytes to be added
     * @throws TierLimitExceededException if the storage limit would be exceeded
     */
    void checkStorageLimit(Long tenantId, long additionalBytes);
}
