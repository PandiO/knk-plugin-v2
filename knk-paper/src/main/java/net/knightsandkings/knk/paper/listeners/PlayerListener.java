package net.knightsandkings.knk.paper.listeners;

import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.knightsandkings.knk.core.cache.UserCache;
import net.knightsandkings.knk.core.domain.users.UserSummary;
import net.knightsandkings.knk.paper.utils.ColorOptions;
import net.kyori.adventure.text.Component;

/**
 * Legacy player listener for join events.
 * 
 * NOTE: User creation is now handled by UserAccountListener + UserManager.
 * This listener only handles join greeting and teleportation.
 */
public class PlayerListener implements Listener {
	private static final Logger LOGGER = Logger.getLogger(PlayerListener.class.getName());

	private final UserCache userCache;

	public PlayerListener(UserCache userCache) {
		this.userCache = userCache;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
        UserSummary user = userCache.getByUuid(player.getUniqueId()).orElse(null);

		e.joinMessage(Component.text("► " + "Player " + player.getName() + " joined").color(ColorOptions.message));
        
        player.sendMessage(Component.text("Welcome back " + player.getName()).color(ColorOptions.messageachievement));
		if (user != null) {
			player.sendMessage(Component.text("You have " + user.coins() + " coins.").color(ColorOptions.messageachievement));
			if (user.isNewUser()) {
				player.sendMessage(Component.text("It looks like this is your first time playing! Enjoy your adventure!").color(ColorOptions.messageachievement));
			}
		}

		if (!player.hasPermission("k&k.join.owner")) {
			player.setGameMode(GameMode.SURVIVAL);
			player.setFlying(false);
			// Town town = (Town) RepositoryManager.getInstance().getRepository(Town.class, Dominion.KEY_CLASS).getList().get(0);
			// if (town != null) {
			// 	user.teleport(town.getLocation().getLocation(), town.getName());
			// } else {
			// 	user.teleport(Bukkit.getWorld(KNK.WORLD_NAME_DEF).getSpawnLocation(), null);
			// }
            player.teleport(Bukkit.getWorld(Bukkit.getWorlds().get(0).getName()).getSpawnLocation());
		}
		// ScoreboardUtil.setScoreboard(Arrays.asList(user));
	}
}
