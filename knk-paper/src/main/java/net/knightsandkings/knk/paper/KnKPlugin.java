package net.knightsandkings.knk.paper;

import net.knightsandkings.knk.api.auth.ApiKeyAuthProvider;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.auth.BearerAuthProvider;
import net.knightsandkings.knk.api.auth.NoAuthProvider;
import net.knightsandkings.knk.api.client.KnkApiClient;
import net.knightsandkings.knk.paper.commands.KnkAdminCommand;
import net.knightsandkings.knk.paper.config.ConfigLoader;
import net.knightsandkings.knk.paper.config.KnkConfig;
import net.knightsandkings.knk.core.ports.api.TownsQueryApi;
import net.knightsandkings.knk.core.ports.api.LocationsQueryApi;
import net.knightsandkings.knk.core.ports.api.DistrictsQueryApi;
import net.knightsandkings.knk.core.ports.api.StreetsQueryApi;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class KnKPlugin extends JavaPlugin {
    private KnkApiClient apiClient;
    private KnkConfig config;
    private TownsQueryApi townsQueryApi;
    private LocationsQueryApi locationsQueryApi;
    private DistrictsQueryApi districtsQueryApi;
    private StreetsQueryApi streetsQueryApi;
    
    @Override
    public void onEnable() {
        try {
            // Load and validate config
            saveDefaultConfig();
            config = ConfigLoader.load(getConfig());
            getLogger().info("Configuration loaded successfully");
            getLogger().info("API Base URL: " + config.api().baseUrl());
            
            // Create auth provider based on config
            AuthProvider authProvider = createAuthProvider(config.api().auth());
            
            // Build API client
            apiClient = KnkApiClient.builder()
                .baseUrl(config.api().baseUrl())
                .authProvider(authProvider)
                .connectTimeout(config.api().timeouts().connectDuration())
                .readTimeout(config.api().timeouts().readDuration())
                .writeTimeout(config.api().timeouts().writeDuration())
                .debugLogging(config.api().debugLogging())
                .allowUntrustedSsl(config.api().allowUntrustedSsl())
                .build();
            
            getLogger().info("API client initialized");
            if (config.api().allowUntrustedSsl()) {
                getLogger().warning("WARNING: SSL certificate validation is DISABLED. Only use in development!");
            }
            
            // Wire TownsQueryApi from client
            this.townsQueryApi = apiClient.getTownsQueryApi();
            this.locationsQueryApi = apiClient.getLocationsQueryApi();
            this.districtsQueryApi = apiClient.getDistrictsQueryApi();
            this.streetsQueryApi = apiClient.getStreetsQueryApi();
            getLogger().info("TownsQueryApi wired from API client");
            getLogger().info("LocationsQueryApi wired from API client");
            getLogger().info("DistrictsQueryApi wired from API client");
            getLogger().info("StreetsQueryApi wired from API client");

            // Register commands
            registerCommands();
            
            getLogger().info("KnightsAndKings Plugin Enabled!");
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin: " + e.getMessage());
            getLogger().severe("Plugin will be disabled. Please fix your config.yml");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (apiClient != null) {
            getLogger().info("Shutting down API client...");
            apiClient.shutdown();
        }
        getLogger().info("KnightsAndKings Plugin Disabled!");
    }
    
    // No extra helpers needed; API client constructs and owns HTTP internals.
    
    private void registerCommands() {
        PluginCommand knkCommand = getCommand("knk");
        if (knkCommand != null) {
            knkCommand.setExecutor(new KnkAdminCommand(this, apiClient.getHealthApi(), townsQueryApi, locationsQueryApi, districtsQueryApi, streetsQueryApi));
            getLogger().info("Registered /knk admin command");
        } else {
            getLogger().warning("Failed to register /knk command - not defined in plugin.yml?");
        }
    }
    
    private AuthProvider createAuthProvider(KnkConfig.AuthConfig authConfig) {
        String type = authConfig.type().toLowerCase();
        return switch (type) {
            case "bearer" -> {
                getLogger().info("Using Bearer token authentication");
                yield new BearerAuthProvider(authConfig.bearerToken());
            }
            case "apikey" -> {
                getLogger().info("Using API Key authentication");
                yield new ApiKeyAuthProvider(authConfig.apiKey(), authConfig.apiKeyHeader());
            }
            default -> {
                getLogger().info("Using no authentication");
                yield new NoAuthProvider();
            }
        };
    }
}
