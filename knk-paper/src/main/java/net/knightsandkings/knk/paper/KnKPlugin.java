package net.knightsandkings.knk.paper;

import net.knightsandkings.knk.api.auth.ApiKeyAuthProvider;
import net.knightsandkings.knk.api.auth.AuthProvider;
import net.knightsandkings.knk.api.auth.BearerAuthProvider;
import net.knightsandkings.knk.api.auth.NoAuthProvider;
import net.knightsandkings.knk.api.client.KnkApiClient;
import net.knightsandkings.knk.paper.commands.KnkAdminCommand;
import net.knightsandkings.knk.paper.config.ConfigLoader;
import net.knightsandkings.knk.paper.config.KnkConfig;
import net.knightsandkings.knk.paper.cache.CacheManager;
import net.knightsandkings.knk.core.ports.api.TownsQueryApi;
import net.knightsandkings.knk.core.ports.api.LocationsQueryApi;
import net.knightsandkings.knk.core.ports.api.DistrictsQueryApi;
import net.knightsandkings.knk.core.ports.api.StreetsQueryApi;
import net.knightsandkings.knk.core.ports.api.StructuresQueryApi;
import net.knightsandkings.knk.core.ports.api.DomainsQueryApi;
import net.knightsandkings.knk.core.regions.RegionTransitionService;
import net.knightsandkings.knk.core.regions.SimpleRegionTransitionService;
import net.knightsandkings.knk.paper.listeners.PlayerListener;
import net.knightsandkings.knk.paper.listeners.WorldGuardRegionListener;
import net.knightsandkings.knk.paper.regions.WorldGuardRegionTracker;
import net.knightsandkings.knk.core.regions.RegionDomainResolver;
import net.knightsandkings.knk.core.ports.gates.GateControlPort;
import net.knightsandkings.knk.paper.gates.PaperGateControlAdapter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class KnKPlugin extends JavaPlugin {
    private KnkApiClient apiClient;
    private KnkConfig config;
    private CacheManager cacheManager;
    private TownsQueryApi townsQueryApi;
    private LocationsQueryApi locationsQueryApi;
    private DistrictsQueryApi districtsQueryApi;
    private StreetsQueryApi streetsQueryApi;
    private StructuresQueryApi structuresQueryApi;
    private DomainsQueryApi domainsQueryApi;
    private ExecutorService regionLookupExecutor;
    
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
            this.structuresQueryApi = apiClient.getStructuresQueryApi();
            this.domainsQueryApi = apiClient.getDomainsQueryApi();
            getLogger().info("TownsQueryApi wired from API client");
            getLogger().info("LocationsQueryApi wired from API client");
            getLogger().info("DistrictsQueryApi wired from API client");
            getLogger().info("StreetsQueryApi wired from API client");
            getLogger().info("StructuresQueryApi wired from API client");
            getLogger().info("DomainsQueryApi wired from API client");
            
            // Initialize cache manager
            this.cacheManager = new CacheManager(config.cache().ttl());
            getLogger().info("Cache manager initialized with TTL: " + config.cache().ttl());

            // Register commands
            registerCommands();

            // Register region listeners (WorldGuard)
            // Create domain resolver for mapping WG region IDs to domain entities
            RegionDomainResolver regionDomainResolver = new RegionDomainResolver(
                townsQueryApi,
                districtsQueryApi,
                structuresQueryApi,
                domainsQueryApi,
                cacheManager.getTownCache(),
                cacheManager.getDistrictCache(),
                cacheManager.getStructureCache()
            );

            // Dedicated executor for region lookup (API prefetch); daemon threads to avoid blocking shutdown.
            regionLookupExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                r -> {
                    Thread t = new Thread(r, "knk-region-lookup");
                    t.setDaemon(true);
                    return t;
                }
            );
            
            // Create gate control adapter for handling gate open/close
            GateControlPort gateControlPort = new PaperGateControlAdapter(this);
            
            // Create region transition service with both resolver and gate control
            RegionTransitionService regionTransitionService = new SimpleRegionTransitionService(regionDomainResolver, gateControlPort);
            
            // Wire tracker and listener
            WorldGuardRegionTracker regionTracker = new WorldGuardRegionTracker(
                regionTransitionService,
                regionDomainResolver,
                regionLookupExecutor,
                this,  // Plugin instance for scheduler access
                Logger.getLogger(WorldGuardRegionTracker.class.getName()),
                true  // Enable console logging; set to false to disable
            );
            registerEvents(regionTracker);
            
            getLogger().info("Region transition service initialized with domain resolver and gate control");

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
        if (cacheManager != null) {
            getLogger().info("Logging final cache metrics...");
            cacheManager.logMetrics();
            cacheManager.clearAll();
        }
        if (apiClient != null) {
            getLogger().info("Shutting down API client...");
            apiClient.shutdown();
        }
        if (regionLookupExecutor != null) {
            regionLookupExecutor.shutdownNow();
        }
        getLogger().info("KnightsAndKings Plugin Disabled!");
    }

    private void registerEvents(WorldGuardRegionTracker regionTracker) {
        var pluginManager = getServer().getPluginManager();
        // Event registration moved to onEnable after region transition service setup

        pluginManager.registerEvents(new WorldGuardRegionListener(regionTracker), this);
        pluginManager.registerEvents(new PlayerListener(), this);
    }
    
    /**
     * Returns the cache manager for accessing cache statistics.
     *
     * @return CacheManager instance
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    // No extra helpers needed; API client constructs and owns HTTP internals.
    
    private void registerCommands() {
        PluginCommand knkCommand = getCommand("knk");
        if (knkCommand != null) {
            knkCommand.setExecutor(new KnkAdminCommand(this, apiClient.getHealthApi(), townsQueryApi, locationsQueryApi, districtsQueryApi, streetsQueryApi, cacheManager));
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
