package net.knightsandkings.knk.core.dataaccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Configurable retry policy for transient API failures.
 * <p>
 * Implements exponential backoff with configurable max attempts, initial delay,
 * and backoff multiplier. Only retries specific retryable exceptions (network timeouts,
 * connection failures) to avoid retrying business logic errors.
 * <p>
 * Optional component; gateways can operate without retry if configured with maxAttempts=1.
 */
public final class RetryPolicy {
    
    private static final Logger LOGGER = Logger.getLogger(RetryPolicy.class.getName());
    
    private static final Set<Class<? extends Throwable>> RETRYABLE_EXCEPTIONS = Set.of(
        SocketTimeoutException.class,
        ConnectException.class
    );
    
    private final int maxAttempts;
    private final Duration initialDelay;
    private final double backoffMultiplier;
    private final Duration maxDelay;
    
    private RetryPolicy(
        int maxAttempts,
        Duration initialDelay,
        double backoffMultiplier,
        Duration maxDelay
    ) {
        this.maxAttempts = maxAttempts;
        this.initialDelay = Objects.requireNonNull(initialDelay);
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelay = Objects.requireNonNull(maxDelay);
    }
    
    /**
     * Create a default retry policy with recommended settings.
     * <ul>
     *   <li>Max attempts: 3</li>
     *   <li>Initial delay: 100ms</li>
     *   <li>Backoff multiplier: 2.0</li>
     *   <li>Max delay: 5000ms</li>
     * </ul>
     *
     * @return Default RetryPolicy instance
     */
    public static RetryPolicy defaultPolicy() {
        return new Builder().build();
    }
    
    /**
     * Create a no-retry policy (single attempt only).
     *
     * @return RetryPolicy that makes no retry attempts
     */
    public static RetryPolicy noRetry() {
        return new Builder().maxAttempts(1).build();
    }
    
    /**
     * Create a builder for custom retry policy configuration.
     *
     * @return RetryPolicy.Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Execute an operation with retry logic.
     * <p>
     * Synchronous execution with exponential backoff. Use {@link #executeAsync}
     * for non-blocking retry.
     *
     * @param operation The operation to execute
     * @param <T> Result type
     * @return The result of the operation
     * @throws Exception if all retry attempts fail
     */
    public <T> T execute(Supplier<T> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                
                if (!isRetryable(e)) {
                    LOGGER.warning("Non-retryable exception encountered: " + e.getClass().getSimpleName());
                    throw e;
                }
                
                if (attempt < maxAttempts) {
                    long delayMs = calculateDelay(attempt);
                    LOGGER.info(String.format(
                        "Retry attempt %d/%d after %dms due to %s: %s",
                        attempt, maxAttempts, delayMs,
                        e.getClass().getSimpleName(), e.getMessage()
                    ));
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        LOGGER.warning(String.format(
            "All %d retry attempts exhausted. Last error: %s",
            maxAttempts, lastException != null ? lastException.getMessage() : "unknown"
        ));
        throw lastException;
    }
    
    /**
     * Execute an operation asynchronously with retry logic.
     * <p>
     * Non-blocking execution suitable for use in async contexts.
     * Retries are scheduled with exponential backoff.
     *
     * @param operation The async operation to execute
     * @param <T> Result type
     * @return CompletableFuture that completes with the result or fails after all retries
     */
    public <T> CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> operation) {
        return executeAsyncAttempt(operation, 1);
    }
    
    private <T> CompletableFuture<T> executeAsyncAttempt(
        Supplier<CompletableFuture<T>> operation,
        int attempt
    ) {
        return operation.get()
            .exceptionallyCompose(error -> {
                if (!isRetryable(error) || attempt >= maxAttempts) {
                    if (attempt >= maxAttempts) {
                        LOGGER.warning(String.format(
                            "All %d async retry attempts exhausted. Last error: %s",
                            maxAttempts, error.getMessage()
                        ));
                    }
                    return CompletableFuture.failedFuture(error);
                }
                
                long delayMs = calculateDelay(attempt);
                LOGGER.info(String.format(
                    "Async retry attempt %d/%d after %dms due to %s: %s",
                    attempt, maxAttempts, delayMs,
                    error.getClass().getSimpleName(), error.getMessage()
                ));
                
                // Schedule next attempt after delay
                CompletableFuture<T> delayed = new CompletableFuture<>();
                CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        executeAsyncAttempt(operation, attempt + 1)
                            .whenComplete((result, nextError) -> {
                                if (nextError != null) {
                                    delayed.completeExceptionally(nextError);
                                } else {
                                    delayed.complete(result);
                                }
                            });
                    });
                return delayed;
            });
    }
    
    /**
     * Check if an exception is retryable.
     *
     * @param throwable The exception to check
     * @return true if the exception should trigger a retry
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        // Check exact type match
        for (Class<? extends Throwable> retryableType : RETRYABLE_EXCEPTIONS) {
            if (retryableType.isInstance(throwable)) {
                return true;
            }
        }
        
        // Check cause recursively
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return isRetryable(cause);
        }
        
        return false;
    }
    
    /**
     * Calculate delay for the given attempt using exponential backoff.
     *
     * @param attempt Current attempt number (1-based)
     * @return Delay in milliseconds
     */
    private long calculateDelay(int attempt) {
        double delayMs = initialDelay.toMillis() * Math.pow(backoffMultiplier, attempt - 1);
        return Math.min((long) delayMs, maxDelay.toMillis());
    }
    
    // ==================== Getters ====================
    
    public int maxAttempts() {
        return maxAttempts;
    }
    
    public Duration initialDelay() {
        return initialDelay;
    }
    
    public double backoffMultiplier() {
        return backoffMultiplier;
    }
    
    public Duration maxDelay() {
        return maxDelay;
    }
    
    // ==================== Builder ====================
    
    public static final class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private double backoffMultiplier = 2.0;
        private Duration maxDelay = Duration.ofMillis(5000);
        
        private Builder() {}
        
        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }
        
        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = Objects.requireNonNull(initialDelay);
            return this;
        }
        
        public Builder backoffMultiplier(double backoffMultiplier) {
            if (backoffMultiplier < 1.0) {
                throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
            }
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }
        
        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = Objects.requireNonNull(maxDelay);
            return this;
        }
        
        public RetryPolicy build() {
            return new RetryPolicy(maxAttempts, initialDelay, backoffMultiplier, maxDelay);
        }
    }
    
    @Override
    public String toString() {
        return "RetryPolicy{" +
            "maxAttempts=" + maxAttempts +
            ", initialDelay=" + initialDelay.toMillis() + "ms" +
            ", backoffMultiplier=" + backoffMultiplier +
            ", maxDelay=" + maxDelay.toMillis() + "ms" +
            '}';
    }
}
