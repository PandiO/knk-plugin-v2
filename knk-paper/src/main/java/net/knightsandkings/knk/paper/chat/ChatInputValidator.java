package net.knightsandkings.knk.paper.chat;

import java.util.regex.Pattern;

/**
 * Validates and sanitizes player input for security and data integrity.
 * Used during chat capture flows to ensure input meets requirements.
 */
public class ChatInputValidator {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 255;
    private static final int MAX_EMAIL_LENGTH = 255;
    
    /**
     * Validate an email address format.
     *
     * @param email the email to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        if (email.length() > MAX_EMAIL_LENGTH) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate a password strength.
     *
     * @param password the password to validate
     * @return true if the password meets minimum requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate an account choice (A or B).
     *
     * @param choice the choice to validate
     * @return true if the choice is valid, false otherwise
     */
    public static boolean isValidAccountChoice(String choice) {
        return choice != null && (choice.equalsIgnoreCase("A") || choice.equalsIgnoreCase("B"));
    }
    
    /**
     * Sanitize input to remove potentially harmful characters while preserving valid content.
     * Removes leading/trailing whitespace and checks for suspicious patterns.
     *
     * @param input the input to sanitize
     * @return the sanitized input, or null if input appears malicious
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove leading and trailing whitespace
        String sanitized = input.trim();
        
        // Check for SQL injection patterns (basic check)
        if (containsSuspiciousPattern(sanitized)) {
            return null;
        }
        
        return sanitized;
    }
    
    /**
     * Check if input contains patterns that might indicate attack attempts.
     */
    private static boolean containsSuspiciousPattern(String input) {
        // Check for common SQL injection keywords
        String lowerInput = input.toLowerCase();
        String[] suspiciousKeywords = {
            "select", "insert", "update", "delete", "drop", "exec", "execute",
            "--", "/*", "*/", "xp_", "sp_", ";", "union", "from", "where"
        };
        
        for (String keyword : suspiciousKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get error message for invalid email.
     */
    public static String getEmailErrorMessage() {
        return "Invalid email format. Please use a valid email address (e.g., example@domain.com)";
    }
    
    /**
     * Get error message for invalid password.
     */
    public static String getPasswordErrorMessage() {
        return "Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters";
    }
}
