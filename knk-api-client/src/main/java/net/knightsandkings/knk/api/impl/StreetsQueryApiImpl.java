package net.knightsandkings.knk.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.dto.*;
import net.knightsandkings.knk.api.mapper.StreetsMapper;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.streets.StreetDetail;
import net.knightsandkings.knk.core.domain.streets.StreetSummary;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.StreetsQueryApi;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * HTTP client implementation of StreetsQueryApi using OkHttp.
 * Supports READ-only operations: search streets and get street by ID.
 */
public class StreetsQueryApiImpl implements StreetsQueryApi {
    private static final Logger LOGGER = Logger.getLogger(StreetsQueryApiImpl.class.getName());

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AuthProvider authProvider;
    private final ExecutorService executor;
    private final boolean debugLogging;

    private static final String STREETS_SEARCH_ENDPOINT = "/Streets/search";
    private static final String STREETS_GET_ENDPOINT = "/Streets";
    private static final int MAX_RESPONSE_SNIPPET_LENGTH = 200;

    public StreetsQueryApiImpl(
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
    public CompletableFuture<Page<StreetSummary>> search(PagedQuery query) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + STREETS_SEARCH_ENDPOINT;
            long startTime = System.currentTimeMillis();

            try {
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
                            "Search streets failed",
                            snippet
                        );
                    }

                    if (responseBody.isEmpty()) {
                        throw new ApiException(url, response.code(), "Empty response body", "");
                    }

                    try {
                        StreetListDtoPagedResultDto resultDto = objectMapper.readValue(
                            responseBody,
                            StreetListDtoPagedResultDto.class
                        );

                        StreetsMapper mapper = new StreetsMapper();
                        return mapper.toPage(resultDto);
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
            } catch (Exception e) {
                throw new ApiException(url, "IO error during Streets search: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<StreetDetail> getById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + STREETS_GET_ENDPOINT + "/" + id;
            long startTime = System.currentTimeMillis();

            try {
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
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
                            "Get street by ID failed",
                            snippet
                        );
                    }

                    if (responseBody.isEmpty()) {
                        throw new ApiException(url, response.code(), "Empty response body", "");
                    }

                    try {
                        StreetDto dto = objectMapper.readValue(responseBody, StreetDto.class);

                        StreetsMapper mapper = new StreetsMapper();
                        return mapper.toDetail(dto);
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
            } catch (Exception e) {
                throw new ApiException(url, "IO error during Street getById: " + e.getMessage(), e);
            }
        }, executor);
    }
}
