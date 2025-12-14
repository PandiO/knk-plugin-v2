package net.knightsandkings.knk.api.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.auth.NoAuthProvider;
import net.knightsandkings.knk.api.impl.HealthApiImpl;
import net.knightsandkings.knk.core.ports.api.HealthApi;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main API client entrypoint. Provides access to all API port implementations.
 */
public class KnkApiClient {
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AuthProvider authProvider;
    private final ExecutorService executor;
    
    private final HealthApi healthApi;
    
    private KnkApiClient(
        String baseUrl,
        OkHttpClient httpClient,
        ObjectMapper objectMapper,
        AuthProvider authProvider,
        ExecutorService executor
    ) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.authProvider = authProvider;
        this.executor = executor;
        
        // Initialize API implementations
        this.healthApi = new HealthApiImpl(baseUrl, httpClient, objectMapper, authProvider, executor);
    }
    
    public HealthApi getHealthApi() {
        return healthApi;
    }
    
    /**
     * Shutdown the client and release resources.
     */
    public void shutdown() {
        executor.shutdown();
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
    
    /**
     * Builder for KnkApiClient.
     */
    public static class Builder {
        private String baseUrl;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(10);
        private Duration writeTimeout = Duration.ofSeconds(10);
        private AuthProvider authProvider = new NoAuthProvider();
        private ExecutorService executor;
        private boolean shutdownExecutorOnClose = false;
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }
        
        public Builder readTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }
        
        public Builder writeTimeout(Duration timeout) {
            this.writeTimeout = timeout;
            return this;
        }
        
        public Builder authProvider(AuthProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }
        
        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            this.shutdownExecutorOnClose = false;
            return this;
        }
        
        public KnkApiClient build() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            
            // Remove trailing slash from baseUrl
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            
            OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout)
                .build();
            
            ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            ExecutorService finalExecutor = executor;
            if (finalExecutor == null) {
                finalExecutor = Executors.newFixedThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors()),
                    r -> {
                        Thread t = new Thread(r, "knk-api-client");
                        t.setDaemon(true);
                        return t;
                    }
                );
                shutdownExecutorOnClose = true;
            }
            
            return new KnkApiClient(baseUrl, httpClient, objectMapper, authProvider, finalExecutor);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
