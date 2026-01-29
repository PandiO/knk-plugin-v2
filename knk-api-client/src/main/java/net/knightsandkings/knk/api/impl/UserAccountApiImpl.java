package net.knightsandkings.knk.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.knightsandkings.knk.api.UserAccountApi;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.dto.user.*;
import net.knightsandkings.knk.core.exception.ApiException;
import net.knightsandkings.knk.api.UserAccountApi;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * Implementation of UserAccountApi for user account management operations.
 * Handles account creation, linking, merging, and authentication flows.
 */
public class UserAccountApiImpl extends BaseApiImpl implements UserAccountApi {
    private static final Logger LOGGER = Logger.getLogger(UserAccountApiImpl.class.getName());
    private static final String USERS_ENDPOINT = "/Users";

    public UserAccountApiImpl(
        String baseUrl,
        OkHttpClient httpClient,
        ObjectMapper objectMapper,
        AuthProvider authProvider,
        ExecutorService executor,
        boolean debugLogging
    ) {
        super(baseUrl, httpClient, objectMapper, authProvider, executor, debugLogging);
    }

    @Override
    public CompletableFuture<UserResponse> createUser(CreateUserRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + USERS_ENDPOINT;
            try {
                String bodyJson = objectMapper.writeValueAsString(request);
                LOGGER.fine("Creating user with payload: " + snippet(bodyJson));
                String responseJson = postJson(url, bodyJson);
                return objectMapper.readValue(responseJson, UserResponse.class);
            } catch (ApiException | IOException e) {
                LOGGER.severe("Failed to create user: " + e.getMessage());
                throw new RuntimeException("Failed to create user", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<DuplicateCheckResponse> checkDuplicate(String uuid, String username) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + USERS_ENDPOINT + "/check-duplicate?uuid=" + 
                urlEncode(uuid) + "&username=" + urlEncode(username);
            try {
                LOGGER.fine("Checking duplicate for UUID: " + uuid + ", username: " + username);
                String responseJson = postJson(url, "{}");
                return objectMapper.readValue(responseJson, DuplicateCheckResponse.class);
            } catch (ApiException | IOException e) {
                LOGGER.severe("Failed to check duplicate: " + e.getMessage());
                throw new RuntimeException("Failed to check duplicate accounts", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<LinkCodeResponse> generateLinkCode(Integer userId) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + USERS_ENDPOINT + "/generate-link-code";
            try {
                LinkCodeRequest request = new LinkCodeRequest(userId);
                String bodyJson = objectMapper.writeValueAsString(request);
                LOGGER.fine("Generating link code for user: " + userId);
                String responseJson = postJson(url, bodyJson);
                return objectMapper.readValue(responseJson, LinkCodeResponse.class);
            } catch (ApiException | IOException e) {
                LOGGER.severe("Failed to generate link code: " + e.getMessage());
                throw new RuntimeException("Failed to generate link code", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ValidateLinkCodeResponse> validateLinkCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + USERS_ENDPOINT + "/validate-link-code/" + urlEncode(code);
            try {
                LOGGER.fine("Validating link code: " + code);
                String responseJson = postJson(url, "{}");
                return objectMapper.readValue(responseJson, ValidateLinkCodeResponse.class);
            } catch (ApiException | IOException e) {
                LOGGER.warning("Failed to validate link code: " + e.getMessage());
                throw new RuntimeException("Failed to validate link code", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> updateEmail(Integer userId, String email) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + USERS_ENDPOINT + "/" + userId + "/update-email";
            try {
                // Create a simple JSON body with the email
                String bodyJson = objectMapper.writeValueAsString(
                    new UpdateEmailRequest(email)
                );
                LOGGER.fine("Updating email for user: " + userId);
                putJson(url, bodyJson);
                return true;
            } catch (ApiException | IOException e) {
                LOGGER.severe("Failed to update email: " + e.getMessage());
                throw new RuntimeException("Failed to update email", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> changePassword(Integer userId, ChangePasswordRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + USERS_ENDPOINT + "/" + userId + "/change-password";
            try {
                String bodyJson = objectMapper.writeValueAsString(request);
                LOGGER.fine("Changing password for user: " + userId);
                putJson(url, bodyJson);
                return true;
            } catch (ApiException | IOException e) {
                LOGGER.severe("Failed to change password: " + e.getMessage());
                throw new RuntimeException("Failed to change password", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<UserResponse> mergeAccounts(Integer primaryId, Integer secondaryId) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + USERS_ENDPOINT + "/merge";
            try {
                MergeAccountsRequest request = new MergeAccountsRequest(primaryId, secondaryId);
                String bodyJson = objectMapper.writeValueAsString(request);
                LOGGER.info("Merging accounts: primary=" + primaryId + ", secondary=" + secondaryId);
                String responseJson = postJson(url, bodyJson);
                return objectMapper.readValue(responseJson, UserResponse.class);
            } catch (ApiException | IOException e) {
                LOGGER.severe("Failed to merge accounts: " + e.getMessage());
                throw new RuntimeException("Failed to merge accounts", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<UserResponse> linkAccount(LinkAccountRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + USERS_ENDPOINT + "/link-account";
            try {
                String bodyJson = objectMapper.writeValueAsString(request);
                LOGGER.fine("Linking account with code: " + snippet(request.linkCode()));
                String responseJson = postJson(url, bodyJson);
                return objectMapper.readValue(responseJson, UserResponse.class);
            } catch (ApiException | IOException e) {
                LOGGER.severe("Failed to link account: " + e.getMessage());
                throw new RuntimeException("Failed to link account", e);
            }
        }, executor);
    }

    /**
     * Internal DTO for update email request.
     */
    private record UpdateEmailRequest(
        String newEmail
    ) {}
    
    /**
     * URL-encode a string for safe inclusion in URLs.
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }
}
