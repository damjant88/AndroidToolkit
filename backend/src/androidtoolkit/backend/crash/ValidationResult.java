package androidtoolkit.backend.crash;

/**
 * Result of a validation operation, indicating success or failure with a descriptive message.
 */
public record ValidationResult(boolean valid, String message) {

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message);
    }
}
