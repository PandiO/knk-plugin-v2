package net.knightsandkings.knk.core.dataaccess;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result wrapper for data access operations.
 * <p>
 * Encapsulates the outcome of a fetch attempt, including success/failure status,
 * the retrieved value (if any), error details, staleness indicator, and data source.
 * Provides a consistent, type-safe surface for handling cache/API interactions.
 *
 * @param <T> Type of the fetched entity
 */
public final class FetchResult<T> {
    
    private final FetchStatus status;
    private final T value;
    private final Throwable error;
    private final boolean isStale;
    private final DataSource source;
    
    private FetchResult(
        FetchStatus status,
        T value,
        Throwable error,
        boolean isStale,
        DataSource source
    ) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.value = value;
        this.error = error;
        this.isStale = isStale;
        this.source = Objects.requireNonNull(source, "source must not be null");
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Create a successful result from cache hit.
     *
     * @param value The cached value (must not be null)
     * @param <T> Value type
     * @return FetchResult with HIT status
     */
    public static <T> FetchResult<T> hit(T value) {
        Objects.requireNonNull(value, "value must not be null for HIT result");
        return new FetchResult<>(FetchStatus.HIT, value, null, false, DataSource.CACHE);
    }
    
    /**
     * Create a successful result from API fetch after cache miss.
     *
     * @param value The fetched value (must not be null)
     * @param <T> Value type
     * @return FetchResult with MISS_FETCHED status
     */
    public static <T> FetchResult<T> missFetched(T value) {
        Objects.requireNonNull(value, "value must not be null for MISS_FETCHED result");
        return new FetchResult<>(FetchStatus.MISS_FETCHED, value, null, false, DataSource.API);
    }
    
    /**
     * Create a not-found result (cache miss + API 404).
     *
     * @param <T> Value type
     * @return FetchResult with NOT_FOUND status
     */
    public static <T> FetchResult<T> notFound() {
        return new FetchResult<>(FetchStatus.NOT_FOUND, null, null, false, DataSource.UNKNOWN);
    }
    
    /**
     * Create an error result with the causing exception.
     *
     * @param error The exception that caused the failure (must not be null)
     * @param <T> Value type
     * @return FetchResult with ERROR status
     */
    public static <T> FetchResult<T> error(Throwable error) {
        Objects.requireNonNull(error, "error must not be null for ERROR result");
        return new FetchResult<>(FetchStatus.ERROR, null, error, false, DataSource.UNKNOWN);
    }
    
    /**
     * Create a stale result (fresh data unavailable, serving cached stale data).
     *
     * @param value The stale cached value (must not be null)
     * @param <T> Value type
     * @return FetchResult with STALE_SERVED status
     */
    public static <T> FetchResult<T> staleServed(T value) {
        Objects.requireNonNull(value, "value must not be null for STALE_SERVED result");
        return new FetchResult<>(FetchStatus.STALE_SERVED, value, null, true, DataSource.CACHE);
    }
    
    // ==================== Accessors ====================
    
    public FetchStatus status() {
        return status;
    }
    
    public Optional<T> value() {
        return Optional.ofNullable(value);
    }
    
    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }
    
    public boolean isStale() {
        return isStale;
    }
    
    public DataSource source() {
        return source;
    }
    
    /**
     * Check if the fetch was successful (HIT, MISS_FETCHED, or STALE_SERVED).
     *
     * @return true if value is present
     */
    public boolean isSuccess() {
        return value != null;
    }
    
    /**
     * Check if the result represents a cache hit (either fresh or stale).
     *
     * @return true if data came from cache
     */
    public boolean isFromCache() {
        return source == DataSource.CACHE;
    }
    
    /**
     * Check if the result represents fresh data from API.
     *
     * @return true if data came from API
     */
    public boolean isFromApi() {
        return source == DataSource.API;
    }
    
    // ==================== Functional Methods ====================
    
    /**
     * Apply a function to the value if present.
     *
     * @param mapper Function to transform the value
     * @param <U> New value type
     * @return FetchResult with transformed value, or original error/not-found result
     */
    public <U> FetchResult<U> map(Function<? super T, ? extends U> mapper) {
        if (value == null) {
            // Preserve status/error/source for non-success results
            return new FetchResult<>(status, null, error, isStale, source);
        }
        U mappedValue = mapper.apply(value);
        return new FetchResult<>(status, mappedValue, error, isStale, source);
    }
    
    /**
     * Execute an action if the fetch was successful.
     *
     * @param consumer Action to perform on the value
     * @return this FetchResult for chaining
     */
    public FetchResult<T> ifSuccess(Consumer<? super T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
        return this;
    }
    
    /**
     * Execute an action if the fetch failed with an error.
     *
     * @param consumer Action to perform on the error
     * @return this FetchResult for chaining
     */
    public FetchResult<T> ifError(Consumer<Throwable> consumer) {
        if (error != null) {
            consumer.accept(error);
        }
        return this;
    }
    
    /**
     * Get the value or throw an exception.
     *
     * @return The value if present
     * @throws IllegalStateException if value is not present
     */
    public T orElseThrow() {
        if (value == null) {
            throw new IllegalStateException(
                "FetchResult value not present. Status: " + status +
                (error != null ? ", Error: " + error.getMessage() : "")
            );
        }
        return value;
    }
    
    /**
     * Get the value or a default.
     *
     * @param defaultValue Value to return if fetch failed
     * @return The fetched value or the default
     */
    public T orElse(T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    // ==================== Object Methods ====================
    
    @Override
    public String toString() {
        return "FetchResult{" +
            "status=" + status +
            ", value=" + (value != null ? value.getClass().getSimpleName() : "null") +
            ", error=" + (error != null ? error.getClass().getSimpleName() + ": " + error.getMessage() : "null") +
            ", isStale=" + isStale +
            ", source=" + source +
            '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FetchResult)) return false;
        FetchResult<?> that = (FetchResult<?>) o;
        return isStale == that.isStale &&
            status == that.status &&
            Objects.equals(value, that.value) &&
            Objects.equals(error, that.error) &&
            source == that.source;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, value, error, isStale, source);
    }
}
