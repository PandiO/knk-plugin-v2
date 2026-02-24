package net.knightsandkings.knk.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.dto.MinecraftMaterialRefDto;
import net.knightsandkings.knk.api.mapper.ItemBlueprintMapper;
import net.knightsandkings.knk.core.domain.material.KnkMinecraftMaterialRef;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.core.ports.api.MinecraftMaterialRefsQueryApi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class MinecraftMaterialRefsQueryApiImpl implements MinecraftMaterialRefsQueryApi {
    private static final Logger LOGGER = Logger.getLogger(MinecraftMaterialRefsQueryApiImpl.class.getName());

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AuthProvider authProvider;
    private final ExecutorService executor;
    private final boolean debugLogging;

    private static final String BASE_ENDPOINT = "/MinecraftMaterialRefs";
    private static final int MAX_RESPONSE_SNIPPET_LENGTH = 200;

    public MinecraftMaterialRefsQueryApiImpl(
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
    public CompletableFuture<KnkMinecraftMaterialRef> getById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + BASE_ENDPOINT + "/" + id;
            long startTime = System.currentTimeMillis();

            Request.Builder requestBuilder = new Request.Builder().url(url);
            if (authProvider != null && authProvider.getAuthHeader() != null) {
                requestBuilder.addHeader(authProvider.getAuthHeaderName(), authProvider.getAuthHeader());
            }

            Request request = requestBuilder.build();
            if (debugLogging) {
                LOGGER.info("API Request: GET " + url);
            }

            try (Response response = httpClient.newCall(request).execute()) {
                long latency = System.currentTimeMillis() - startTime;
                if (debugLogging) {
                    LOGGER.info(String.format("API Response: GET %s [%d] in %dms", url, response.code(), latency));
                }

                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    String snippet = responseBody.substring(0, Math.min(responseBody.length(), MAX_RESPONSE_SNIPPET_LENGTH));
                    if (responseBody.length() > MAX_RESPONSE_SNIPPET_LENGTH) snippet += "...";
                    throw new ApiException(url, response.code(), "Get material ref by ID failed", snippet);
                }

                if (responseBody.isEmpty()) {
                    throw new ApiException(url, response.code(), "Empty response body", "");
                }

                try {
                    MinecraftMaterialRefDto dto = objectMapper.readValue(responseBody, MinecraftMaterialRefDto.class);
                    return ItemBlueprintMapper.toCore(dto);
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
                        "IO error during get material ref by ID",
                        e.getClass().getSimpleName() + ": " + e.getMessage()
                );
                apiEx.initCause(e);
                throw apiEx;
            }
        }, executor);
    }
}
