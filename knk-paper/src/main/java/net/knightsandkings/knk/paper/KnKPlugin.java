package net.knightsandkings.knk.paper;
import org.bukkit.plugin.java.JavaPlugin;

public class KnKPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("KnightsAndKings Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("KnightsAndKings Plugin Disabled!");
    }
}