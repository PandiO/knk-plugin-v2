package net.knightsandkings.knk.api.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.core.domain.domains.DomainRegionQuery;
import net.knightsandkings.knk.core.domain.domains.DomainRegionSummary;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.DomainsQueryApi;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DomainsQueryApiImpl implements DomainsQueryApi {
    private static final Logger LOGGER = Logger.getLogger(DistrictsQueryApiImpl.class.getName());
    
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AuthProvider authProvider;
    private final ExecutorService executor;
    private final boolean debugLogging;
    
    private static final String DOMAINS_GET_BY_REGION_ID_ENDPOINT = "/Domains/by-region";
    private static final String DOMAINS_SEARCH_REGION_DECISIONS_ENDPOINT = "/Domains/search-region-decisions";
    private static final int MAX_RESPONSE_SNIPPET_LENGTH = 200;
    private static final MediaType JSON = MediaType.get("application/json");

    public DomainsQueryApiImpl(
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
    public CompletableFuture<DomainRegionSummary> getByWorldGuardRegionId(String wgRegionId) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + DOMAINS_GET_BY_REGION_ID_ENDPOINT + "/" + wgRegionId;
            long startTime = System.currentTimeMillis();

            try {
                
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .get();
                
                // Add auth header if provider is configured
                if (authProvider != null && authProvider.getAuthHeader() != null) {
                    requestBuilder.addHeader(
                        authProvider.getAuthHeaderName(),
                        authProvider.getAuthHeader()
                    );
                }
                
                Request request = requestBuilder.build();
                
                if (debugLogging) {
                    LOGGER.info("Sending GET request to: " + url);
                }
                
                try (Response response = httpClient.newCall(request).execute()) {
                    long latency = System.currentTimeMillis() - startTime;

                    if (debugLogging) {
                        LOGGER.info(String.format("API Response: GET %s [%d] in %dms",
                        url, response.code(), latency));
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (debugLogging) {
                        LOGGER.info("  Response body: " + responseBody.substring(0, Math.min(responseBody.length(), 500)));
                    }
                    
                    if (!response.isSuccessful()) {
                        String snippet = responseBody.substring(0, Math.min(responseBody.length(), MAX_RESPONSE_SNIPPET_LENGTH));
                        if (responseBody.length() > MAX_RESPONSE_SNIPPET_LENGTH) {
                            snippet += "...";
                        }
                        
                        throw new ApiException(
                            url,
                            response.code(),
                            "Domain fetch by WG region ID failed",
                            snippet
                        );
                    }
                    
                    if (responseBody.isEmpty()) {
                        throw new ApiException(url, response.code(), "Empty response body", "");
                    }
                    
                    try {
                        DomainRegionSummary domainSummary = objectMapper.readValue(responseBody, DomainRegionSummary.class);
                        return domainSummary;
                    }  catch (Exception parseEx) {
                        LOGGER.warning("Failed to parse response: " + responseBody);
                        throw new ApiException(
                            url,
                            response.code(),
                            "Failed to parse response: " + parseEx.getMessage(),
                            responseBody.substring(0, Math.min(responseBody.length(), MAX_RESPONSE_SNIPPET_LENGTH))
                        );
                    }

                }
            } catch (ApiException e) {
                throw e;
            } catch (IOException e) {
                ApiException apiEx = new ApiException(
                    url,
                    0,
                    "IO error during search districts",
                    e.getClass().getSimpleName() + ": " + e.getMessage()
                );
                apiEx.initCause(e);
                throw apiEx;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<HashMap<Integer, DomainRegionSummary>> searchDomainRegionDecisions(DomainRegionQuery query) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + DOMAINS_SEARCH_REGION_DECISIONS_ENDPOINT;
            long startTime = System.currentTimeMillis();

            String payload;
            try {
                payload = objectMapper.writeValueAsString(query);
            } catch (JsonProcessingException e) {
                throw new ApiException(url, 0, "Failed to serialize domain search request", e.getMessage());
            }

            try {
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(payload, JSON));

                if (authProvider != null && authProvider.getAuthHeader() != null) {
                    requestBuilder.addHeader(authProvider.getAuthHeaderName(), authProvider.getAuthHeader());
                }

                Request request = requestBuilder.build();

                if (debugLogging) {
                    LOGGER.info("Sending POST request to: " + url + " with payload: " + payload);
                }

                try (Response response = httpClient.newCall(request).execute()) {
                    long latency = System.currentTimeMillis() - startTime;

                    if (debugLogging) {
                        LOGGER.info(String.format("API Response: POST %s [%d] in %dms",
                        url, response.code(), latency));
                    }

                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (debugLogging) {
                        LOGGER.info("  Response body: " + responseBody.substring(0, Math.min(responseBody.length(), 500)));
                    }

                    if (!response.isSuccessful()) {
                        String snippet = responseBody.substring(0, Math.min(responseBody.length(), MAX_RESPONSE_SNIPPET_LENGTH));
                        if (responseBody.length() > MAX_RESPONSE_SNIPPET_LENGTH) {
                            snippet += "...";
                        }

                        throw new ApiException(
                            url,
                            response.code(),
                            "Domain search by WG region IDs failed",
                            snippet
                        );
                    }

                    if (responseBody.isEmpty()) {
                        throw new ApiException(url, response.code(), "Empty response body", "");
                    }

                    try {
                        HashMap<Integer, DomainRegionSummary> summaries = objectMapper.readValue(
                            responseBody,
                            objectMapper.getTypeFactory().constructMapType(HashMap.class, Integer.class, DomainRegionSummary.class)
                        );
                        return summaries;
                    }  catch (Exception parseEx) {
                        LOGGER.warning("Failed to parse response: " + responseBody);
                        throw new ApiException(
                            url,
                            response.code(),
                            "Failed to parse response: " + parseEx.getMessage(),
                            responseBody.substring(0, Math.min(responseBody.length(), MAX_RESPONSE_SNIPPET_LENGTH))
                        );
                    }

                }
            } catch (ApiException e) {
                throw e;
            } catch (IOException e) {
                ApiException apiEx = new ApiException(
                    url,
                    0,
                    "IO error during domain search",
                    e.getClass().getSimpleName() + ": " + e.getMessage()
                );
                apiEx.initCause(e);
                throw apiEx;
            }
        }, executor);
    }
    
}
