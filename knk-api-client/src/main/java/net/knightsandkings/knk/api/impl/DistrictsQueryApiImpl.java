package net.knightsandkings.knk.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.dto.DistrictDto;
import net.knightsandkings.knk.api.dto.DistrictListDtoPagedResultDto;
import net.knightsandkings.knk.api.dto.PagedQueryDto;
import net.knightsandkings.knk.api.mapper.DistrictsMapper;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.districts.DistrictDetail;
import net.knightsandkings.knk.core.domain.districts.DistrictSummary;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.DistrictsQueryApi;
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
 * HTTP client implementation of DistrictsQueryApi using OkHttp.
 * Supports READ-only operations: search districts and get district by ID.
 */
public class DistrictsQueryApiImpl implements DistrictsQueryApi {
    private static final Logger LOGGER = Logger.getLogger(DistrictsQueryApiImpl.class.getName());
    
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AuthProvider authProvider;
    private final ExecutorService executor;
    private final boolean debugLogging;
    
    private static final String DISTRICTS_BASE_ENDPOINT = "/Districts";
    private static final String DISTRICTS_SEARCH_ENDPOINT = "/Districts/search";
    private static final int MAX_RESPONSE_SNIPPET_LENGTH = 200;
    
    public DistrictsQueryApiImpl(
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
    public CompletableFuture<Page<DistrictSummary>> search(net.knightsandkings.knk.core.domain.common.PagedQuery query) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + DISTRICTS_SEARCH_ENDPOINT;
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
                            "Search districts failed",
                            snippet
                        );
                    }
                    
                    if (responseBody.isEmpty()) {
                        throw new ApiException(url, response.code(), "Empty response body", "");
                    }
                    
                    try {
                        DistrictListDtoPagedResultDto result = objectMapper.readValue(
                            responseBody,
                            DistrictListDtoPagedResultDto.class
                        );
                        return DistrictsMapper.mapPagedList(result);
                    } catch (Exception parseEx) {
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
    public CompletableFuture<DistrictDetail> getById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + DISTRICTS_BASE_ENDPOINT + "/" + id;
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
                        "Get district by ID failed",
                        snippet
                    );
                }
                
                if (responseBody.isEmpty()) {
                    throw new ApiException(url, response.code(), "Empty response body", "");
                }
                
                try {
                    DistrictDto dto = objectMapper.readValue(responseBody, DistrictDto.class);
                    return DistrictsMapper.mapDetail(dto);
                } catch (Exception parseEx) {
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
                ApiException apiEx = new ApiException(
                    url,
                    0,
                    "IO error getting district by ID",
                    e.getClass().getSimpleName() + ": " + e.getMessage()
                );
                apiEx.initCause(e);
                throw apiEx;
            }
        }, executor);
    }
}
