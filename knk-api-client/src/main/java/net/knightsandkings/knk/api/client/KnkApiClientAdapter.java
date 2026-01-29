package net.knightsandkings.knk.api.client;

import net.knightsandkings.knk.core.ports.api.UserAccountApi;

/**
 * Adapter to work around gradle compilation issue with KnkApiClient.
 * Provides access to the UserAccountApi that should be available from KnkApiClient.
 */
public class KnkApiClientAdapter {
    private final KnkApiClient client;

    public KnkApiClientAdapter(KnkApiClient client) {
        this.client = client;
    }

    /**
     * Get the UserAccountApi from the KnkApiClient.
     * This is a workaround for a gradle compilation issue where the getUserAccountApi()
     * method is not being compiled into the bytecode despite being in the source code.
     */
    public UserAccountApi getUserAccountApi() {
        // Use reflection as a last resort since the method isn't in the compiled bytecode
        try {
            return (UserAccountApi) client.getClass()
                .getMethod("getUserAccountApi")
                .invoke(client);
        } catch (Exception e) {
            // If reflection fails, create a null placeholder or throw
            throw new RuntimeException("Failed to get UserAccountApi from KnkApiClient", e);
        }
    }
}
