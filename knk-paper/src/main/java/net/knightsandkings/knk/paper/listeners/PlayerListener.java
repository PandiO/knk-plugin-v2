package net.knightsandkings.knk.paper.listeners;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import net.knightsandkings.knk.core.cache.UserCache;
import net.knightsandkings.knk.core.domain.users.UserSummary;
import net.knightsandkings.knk.core.domain.users.UserDetail;
import net.knightsandkings.knk.core.ports.api.UsersCommandApi;
import net.knightsandkings.knk.core.ports.api.UsersQueryApi;
import net.knightsandkings.knk.paper.utils.ColorOptions;
import net.kyori.adventure.text.Component;

public class PlayerListener implements Listener {
	private static final Logger LOGGER = Logger.getLogger(PlayerListener.class.getName());

	private final UsersQueryApi usersQueryApi;
	private final UsersCommandApi usersCommandApi;
	private final UserCache userCache;

	public PlayerListener(UsersQueryApi usersQueryApi, UsersCommandApi usersCommandApi, UserCache userCache) {
		this.usersQueryApi = usersQueryApi;
		this.userCache = userCache;
		this.usersCommandApi = usersCommandApi;
	}

	@EventHandler
	public void onValidateLogin(AsyncPlayerPreLoginEvent e) {
		UUID uuid = e.getUniqueId();

		// Check cache first
		if (userCache.getByUuid(uuid).isPresent()) {
			LOGGER.fine("User " + uuid + " found in cache");
			return;
		}

		// Fetch from API and cache (already on async thread - safe for blocking)
		try {
			UserSummary user = usersQueryApi.getByUuid(uuid).join();
			if (user == null) {
				LOGGER.warning("User " + uuid + " not found in API");
				user = usersQueryApi.getByUsername(e.getName()).join();
				if (user == null) {
					LOGGER.warning("User " + uuid + " with username " + e.getName() + " not found in API");
				}

				UserDetail newUser = new UserDetail(null, e.getName(), uuid, null, -1, new Date(), true);
				usersCommandApi.create(newUser).whenComplete((createdUser, ex) -> {
					if (ex != null) {
						LOGGER.severe("Failed to create new user " + uuid + ": " + ex.getMessage());
						return;
					}
					int coins = createdUser.coins() != null ? createdUser.coins() : 0;
					UserSummary createdSummary = new UserSummary(createdUser.id(), createdUser.username(), createdUser.uuid(), coins, true);
					userCache.put(createdSummary);
					LOGGER.info("Created and cached new user " + createdUser.username() + " (UUID: " + uuid + ")");
				}).join();
				return;
			}
			userCache.put(user);
			LOGGER.info("Loaded user " + user.username() + " (UUID: " + uuid + ") from API");
		} catch (Exception ex) {
			LOGGER.warning("Failed to load user " + uuid + " from API: " + ex.getMessage());
			LOGGER.fine("Exception details: ");
			ex.printStackTrace();
			// Allow login to proceed even if API fetch fails
		}
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
