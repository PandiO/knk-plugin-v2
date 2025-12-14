package net.knightsandkings.knk.paper;

import net.knightsandkings.knk.api.auth.ApiKeyAuthProvider;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.auth.BearerAuthProvider;
import net.knightsandkings.knk.api.auth.NoAuthProvider;
import net.knightsandkings.knk.api.client.KnkApiClient;
import net.knightsandkings.knk.paper.commands.HealthCommand;
import net.knightsandkings.knk.paper.commands.TownsDebugCommand;
import net.knightsandkings.knk.paper.config.ConfigLoader;
import net.knightsandkings.knk.paper.config.KnkConfig;
import net.knightsandkings.knk.core.ports.api.TownsQueryApi;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class KnKPlugin extends JavaPlugin {
    private KnkApiClient apiClient;
    private KnkConfig config;
    private TownsQueryApi townsQueryApi;
    
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
            getLogger().info("TownsQueryApi wired from API client");

            // Register commands
            registerCommands();
            PluginCommand townsCmd = getCommand("knk-towns");
            if (townsCmd != null) {
                townsCmd.setExecutor(new TownsDebugCommand(this, townsQueryApi));
                getLogger().info("Registered /knk-towns debug command");
            } else {
                getLogger().warning("Failed to register /knk-towns command - not defined in plugin.yml?");
            }
            
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
            knkCommand.setExecutor(new HealthCommand(this, apiClient.getHealthApi()));
            getLogger().info("Registered /knk health command");
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
