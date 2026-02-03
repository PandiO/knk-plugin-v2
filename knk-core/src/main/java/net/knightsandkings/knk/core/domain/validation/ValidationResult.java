package net.knightsandkings.knk.core.domain.validation;

/**
 * Result of validation execution.
 * Used to communicate validation status between handlers and display to players.
 */
public class ValidationResult {
    private final boolean isValid;
    private final String message;
    private final boolean isBlocking;

    /**
     * Create a validation result.
     * 
     * @param isValid Whether validation passed
     * @param message Validation message (error or success)
     * @param isBlocking Whether this validation blocks task completion
     */
    public ValidationResult(boolean isValid, String message, boolean isBlocking) {
        this.isValid = isValid;
        this.message = message;
        this.isBlocking = isBlocking;
    }

    /**
     * Create a successful validation result.
     * 
     * @param message Success message
     * @return ValidationResult with isValid=true, isBlocking=false
     */
    public static ValidationResult success(String message) {
        return new ValidationResult(true, message, false);
    }

    /**
     * Create a blocking failure validation result.
     * 
     * @param message Error message
     * @return ValidationResult with isValid=false, isBlocking=true
     */
    public static ValidationResult blockingFailure(String message) {
        return new ValidationResult(false, message, true);
    }

    /**
     * Create a non-blocking warning validation result.
     * 
     * @param message Warning message
     * @return ValidationResult with isValid=false, isBlocking=false
     */
    public static ValidationResult warning(String message) {
        return new ValidationResult(false, message, false);
    }

    public boolean isValid() {
        return isValid;
    }

    public String getMessage() {
        return message;
    }

    public boolean isBlocking() {
        return isBlocking;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "isValid=" + isValid +
                ", message='" + message + '\'' +
                ", isBlocking=" + isBlocking +
                '}';
    }
}
