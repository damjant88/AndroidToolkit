package androidtoolkit.domain.tenant;

/**
 * The subscription tier assigned to a tenant, determining feature
 * availability and resource limits.
 */
public enum SubscriptionTier {

    /**
     * Free tier: 1 user, 2 devices, 5 AI analyses/month, 500 MB storage.
     */
    FREE,

    /**
     * Pro tier: 20 users, unlimited devices, unlimited analyses, 50 GB storage.
     */
    PRO,

    /**
     * Enterprise tier: no limits, SSO integration, audit logging.
     */
    ENTERPRISE
}
