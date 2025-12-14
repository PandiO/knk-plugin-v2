package net.knightsandkings.knk.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.dto.PagedQueryDto;
import net.knightsandkings.knk.api.dto.TownDto;
import net.knightsandkings.knk.api.dto.TownListDtoPagedResultDto;
import net.knightsandkings.knk.api.mapper.TownsMapper;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.towns.TownDetail;
import net.knightsandkings.knk.core.domain.towns.TownSummary;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.TownsQueryApi;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * HTTP client implementation of TownsQueryApi using OkHttp.
 * Supports READ-only operations: list towns (via search) and get town by ID.
 */
public class TownsQueryApiImpl implements TownsQueryApi {
    private static final Logger LOGGER = Logger.getLogger(TownsQueryApiImpl.class.getName());
    
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AuthProvider authProvider;
    private final ExecutorService executor;
    private final boolean debugLogging;
    
    // Note: baseUrl already includes /api prefix, so endpoints should not include it
    private static final String TOWNS_BASE_ENDPOINT = "/Towns";
    private static final String TOWNS_SEARCH_ENDPOINT = "/Towns/search";
    private static final int MAX_RESPONSE_SNIPPET_LENGTH = 200;
    
    public TownsQueryApiImpl(
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
    public CompletableFuture<Page<TownSummary>> search(net.knightsandkings.knk.core.domain.common.PagedQuery query) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + TOWNS_SEARCH_ENDPOINT;
            long startTime = System.currentTimeMillis();
            
            try {
                // Convert domain PagedQuery to DTO
                PagedQueryDto dto = new PagedQueryDto(
                    query.pageNumber(),
                    query.pageSize(),
                    query.searchTerm(),
                    query.sortBy(),
                    query.sortDescending(),
                    query.filters()
                );
                
                String requestBody = objectMapper.writeValueAsString(dto);
                
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")));
                
                // Add auth header if provider is configured
                if (authProvider != null && authProvider.getAuthHeader() != null) {
                    requestBuilder.addHeader(
                        authProvider.getAuthHeaderName(),
                        authProvider.getAuthHeader()
                    );
                }
                
                Request request = requestBuilder.build();
                
                if (debugLogging) {
                    LOGGER.info("API Request: POST " + url);
                    LOGGER.info("  Body: " + requestBody);
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
                            "Search towns failed",
                            snippet
                        );
                    }
                    
                    if (responseBody.isEmpty()) {
                        throw new ApiException(url, response.code(), "Empty response body", "");
                    }
                    
                    try {
                        TownListDtoPagedResultDto result = objectMapper.readValue(
                            responseBody,
                            TownListDtoPagedResultDto.class
                        );
                        return TownsMapper.mapPagedList(result);
                    } catch (Exception parseEx) {
                        // Log the actual response for debugging deserialization issues
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
                // Preserve the actual IOException as the cause for better debugging
                ApiException apiEx = new ApiException(
                    url,
                    0,
                    "IO error during search towns",
                    e.getClass().getSimpleName() + ": " + e.getMessage()
                );
                apiEx.initCause(e);
                throw apiEx;
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<TownDetail> getById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + TOWNS_BASE_ENDPOINT + "/" + id;
            long startTime = System.currentTimeMillis();
            
            Request.Builder requestBuilder = new Request.Builder().url(url);
            
            // Add auth header if provider is configured
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
                        "Get town by ID failed",
                        snippet
                    );
                }
                
                if (responseBody.isEmpty()) {
                    throw new ApiException(url, response.code(), "Empty response body", "");
                }
                
                try {
                    TownDto dto = objectMapper.readValue(responseBody, TownDto.class);
                    return TownsMapper.mapDetail(dto);
                } catch (Exception parseEx) {
                    // Log the actual response for debugging deserialization issues
                    LOGGER.warning("Failed to parse response: " + responseBody);
                    throw new ApiException(
                        url,
                        response.code(),
                        "Failed to parse response: " + parseEx.getMessage(),
                        responseBody.substring(0, Math.min(responseBody.length(), MAX_RESPONSE_SNIPPET_LENGTH))
                    );
                }
            } catch (ApiException e) {
                throw e;
            } catch (IOException e) {
                // Preserve the actual IOException as the cause for better debugging
                ApiException apiEx = new ApiException(
                    url,
                    0,
                    "IO error during get town by ID",
                    e.getClass().getSimpleName() + ": " + e.getMessage()
                );
                apiEx.initCause(e);
                throw apiEx;
            }
        }, executor);
    }
}
