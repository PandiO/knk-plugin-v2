package net.knightsandkings.knk.paper.listeners;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.knightsandkings.knk.core.domain.districts.DistrictDetail.Town;
import net.knightsandkings.knk.core.domain.towns.TownDetail;
import net.knightsandkings.knk.core.domain.users.UserDetail;
import net.knightsandkings.knk.core.domain.users.UserSummary;
import net.knightsandkings.knk.core.ports.api.UsersCommandApi;
import net.knightsandkings.knk.core.ports.api.UsersQueryApi;
import net.knightsandkings.knk.paper.KnKPlugin;
import net.knightsandkings.knk.paper.cache.CacheManager;
import net.knightsandkings.knk.paper.utils.ColorOptions;
import net.knightsandkings.knk.paper.utils.ScoreboardUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class PlayerListener implements Listener {
	private static final Logger LOGGER = Logger.getLogger(PlayerListener.class.getName());
	private static final long MENTION_SOUND_COOLDOWN_MILLIS = 5_000L;
	private static final Map<UUID, Long> mentionSoundCooldowns = new ConcurrentHashMap<>();

	private final UsersQueryApi usersQueryApi;
	private final UsersCommandApi usersCommandApi;
	private final CacheManager cacheManager;

	public PlayerListener(UsersQueryApi usersQueryApi, UsersCommandApi usersCommandApi, CacheManager cacheManager) {
		this.usersQueryApi = usersQueryApi;
		this.cacheManager = cacheManager;
		this.usersCommandApi = usersCommandApi;
	}

	@EventHandler
	public void onValidateLogin(AsyncPlayerPreLoginEvent e) {
		UUID uuid = e.getUniqueId();

		// Check cache first
		if (cacheManager.getUserCache().getByUuid(uuid).isPresent()) {
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
					UserSummary createdSummary = new UserSummary(createdUser.id(), createdUser.username(), createdUser.uuid(), createdUser.coins(), true);
					cacheManager.getUserCache().put(createdSummary);
					LOGGER.info("Created and cached new user " + createdUser.username() + " (UUID: " + uuid + ")");
				}).join();
				return;
			}
			cacheManager.getUserCache().put(user);
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
        UserSummary user = cacheManager.getUserCache().getByUuid(player.getUniqueId()).orElse(null);

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
		ScoreboardUtil.setScoreboard(Arrays.asList(player));
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		e.quitMessage(Component.text(ColorOptions.messageArrow + "Player " + player.getName() + " left").color(ColorOptions.message));
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();
		String cmd = e.getMessage();
		if (cmd.equalsIgnoreCase("/help")
				|| cmd.equalsIgnoreCase("/plugins")
				|| cmd.equalsIgnoreCase("/pl")
				|| cmd.equalsIgnoreCase("/plugin")
				|| cmd.equalsIgnoreCase("/v")
				|| cmd.equalsIgnoreCase("/version")) {
			if (!player.hasPermission("k&k.owner")) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onChat(AsyncChatEvent e) {
		Player player = e.getPlayer();

		String rawMessage = PlainTextComponentSerializer.plainText().serialize(e.message());
		String dn = player.getName();
		String capitalizedMessage = rawMessage.isEmpty() ? rawMessage : ("" + rawMessage.charAt(0)).toUpperCase() + rawMessage.substring(1);
		
		// Convert legacy color codes (&c, &4, etc.) to Adventure Component
		String legacyFormattedMessage = ChatColor.translateAlternateColorCodes('&', capitalizedMessage);
		Component messageComponent = LegacyComponentSerializer.legacySection().deserialize(legacyFormattedMessage);

		Component finalMessage;
		if (player.hasPermission("k&k.owner")) {
			// Build owner format with proper Components using ColorOptions TextColor objects
			Component prefixComponent = Component.text("[")
					.color(ColorOptions.ownerformat)
					.append(Component.text("OWNER").color(ColorOptions.ownersubjects))
					.append(Component.text("]").color(ColorOptions.ownerformat))
					.append(Component.text(" " + dn + ": ").color(ColorOptions.ownersubjects));
			finalMessage = prefixComponent.append(messageComponent);
			e.renderer((source, sourceDisplayName, message, viewer) -> finalMessage);
		} else {
			// Build default format with proper Components using ColorOptions TextColor objects
			Component prefixComponent = Component.text(" " + dn + ": ")
					.color(ColorOptions.defaultsubjects);
			finalMessage = prefixComponent.append(messageComponent);
			e.renderer((source, sourceDisplayName, message, viewer) -> finalMessage);
		}

		/**
		 * Player mention
		 */
		new BukkitRunnable() {
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				String lowered = rawMessage.toLowerCase();
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (!lowered.contains(p.getName().toLowerCase())) {
						continue;
					}
					Long last = mentionSoundCooldowns.get(p.getUniqueId());
					if (last != null && now - last < MENTION_SOUND_COOLDOWN_MILLIS) {
						continue;
					}
					mentionSoundCooldowns.put(p.getUniqueId(), now);
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
				}
			}
		}.runTaskAsynchronously(KnKPlugin.getPlugin(KnKPlugin.class));
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();
		Player killer = player.getKiller();
		player.sendMessage(Component.text("You died").color(ColorOptions.message));
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		Player player = e.getPlayer();
		/**
		 * TODO Should be replaced by a more safe way to fetch the default town.
		 * Even better would be to load default values for spawn locations and other default settings from the DB and store them in a local yml file.
		 * This would ensure the server can still function in a safe-mode state when the connection with the Api fails.
		 */
		TownDetail town = this.cacheManager.getTownCache().getById(4).orElse(null);
		if (town == null) {
			LOGGER.severe("Failed to load default town to respawn player at.");
			// e.setRespawnLocation(Bukkit.getCurrentSpawnpoint().getLocation());
			return;
		}
		/*
		* TODO Implement logic to retrieve Bukkit Location from KnK location entity to set respawn location.
		*/
		// e.setRespawnLocation()
	}

	@EventHandler
	public void onItemPickup(PlayerPickupItemEvent e) {
		e.setCancelled(true);
	}
}
