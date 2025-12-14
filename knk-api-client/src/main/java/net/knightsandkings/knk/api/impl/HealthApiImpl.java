package net.knightsandkings.knk.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.dto.HealthStatusDto;
import net.knightsandkings.knk.api.mapper.HealthStatusMapper;
import net.knightsandkings.knk.core.domain.HealthStatus;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.HealthApi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * HTTP client implementation of HealthApi using OkHttp.
 */
public class HealthApiImpl implements HealthApi {
    private static final Logger LOGGER = Logger.getLogger(HealthApiImpl.class.getName());
    
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AuthProvider authProvider;
    private final ExecutorService executor;
    private final boolean debugLogging;
    
    // TODO: API contract unknown; using default endpoint
    private static final String HEALTH_ENDPOINT = "/health";
    private static final int MAX_RESPONSE_SNIPPET_LENGTH = 200;
    
    public HealthApiImpl(
        String baseUrl,
        OkHttpClient httpClient,
        ObjectMapper objectMapper,
        AuthProvider authProvider,
        ExecutorService executor,
        boolean debugLogging
    ) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.authProvider = authProvider;
        this.executor = executor;
        this.debugLogging = debugLogging;
    }
    
    @Override
    public CompletableFuture<HealthStatus> getHealth() {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + HEALTH_ENDPOINT;
            long startTime = System.currentTimeMillis();
            
            Request.Builder requestBuilder = new Request.Builder().url(url);
            
            // Add auth header if provider is configured
            // Note: We don't log the actual auth header value to avoid leaking secrets
            if (authProvider != null && authProvider.getAuthHeader() != null) {
                requestBuilder.addHeader(
                    authProvider.getAuthHeaderName(),
                    authProvider.getAuthHeader()
                );
            }
            
            Request request = requestBuilder.build();
            
            if (debugLogging) {
                LOGGER.info("API Request: GET " + url);
            }
            
            try (Response response = httpClient.newCall(request).execute()) {
                long latency = System.currentTimeMillis() - startTime;
                
                if (debugLogging) {
                    LOGGER.info(String.format("API Response: GET %s [%d] in %dms", 
                        url, response.code(), latency));
                }
                
                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    String snippet = body.substring(0, Math.min(body.length(), MAX_RESPONSE_SNIPPET_LENGTH));
                    if (body.length() > MAX_RESPONSE_SNIPPET_LENGTH) {
                        snippet += "...";
                    }
                    
                    throw new ApiException(
                        url,
                        response.code(),
                        "Health check failed",
                        snippet
                    );
                }
                
                if (response.body() == null) {
                    throw new ApiException("Health check response body is null");
                }
                
                String responseBody = response.body().string();
                
                // Handle empty response (some APIs return 200 OK with empty body for health)
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    // Return default healthy status
                    return new HealthStatus("ok", null);
                }
                
                HealthStatusDto dto = objectMapper.readValue(responseBody, HealthStatusDto.class);
                return HealthStatusMapper.toDomain(dto);
                
            } catch (ConnectException | SocketTimeoutException e) {
                // For connection/timeout errors, wrap with URL context
                throw new ApiException(url, "Failed to connect to API: " + e.getClass().getSimpleName(), e);
            } catch (IOException e) {
                throw new ApiException(url, "Failed to execute health check request", e);
            }
        }, executor);
    }
}
