package androidtoolkit.domain.tenant;

/**
 * The outcome of an audited action.
 */
public enum AuditOutcome {

    /**
     * The action completed successfully.
     */
    SUCCESS,

    /**
     * The action failed (e.g., rejected, error occurred).
     */
    FAILURE
}
