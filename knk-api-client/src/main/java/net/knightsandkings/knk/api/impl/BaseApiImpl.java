package net.knightsandkings.knk.api.impl;

import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.knightsandkings.knk.api.auth.AuthProvider;
import okhttp3.OkHttpClient;

public class BaseApiImpl {
    protected static Logger LOGGER = Logger.getLogger(BaseApiImpl.class.getName());
    protected static final int MAX_RESPONSE_SNIPPET_LENGTH = 200;
    
    protected final String baseUrl;
    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected final AuthProvider authProvider;
    protected final ExecutorService executor;
    protected final boolean debugLogging;

    protected BaseApiImpl(
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
}
