package net.knightsandkings.knk.core.exception;

/**
 * Exception thrown when API communication fails.
 */
public class ApiException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;
    private final String requestUrl;
    
    public ApiException(String message) {
        super(message);
        this.statusCode = -1;
        this.responseBody = null;
        this.requestUrl = null;
    }
    
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
        this.requestUrl = null;
    }
    
    public ApiException(String requestUrl, int statusCode, String message, String responseBody) {
        super(String.format("API error [%d]: %s", statusCode, message));
        this.requestUrl = requestUrl;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public ApiException(String requestUrl, String message, Throwable cause) {
        super(message, cause);
        this.requestUrl = requestUrl;
        this.statusCode = -1;
        this.responseBody = null;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getResponseBody() {
        return responseBody;
    }
    
    public String getRequestUrl() {
        return requestUrl;
    }
}
