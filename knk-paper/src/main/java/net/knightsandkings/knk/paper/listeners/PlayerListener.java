package net.knightsandkings.knk.paper.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.kyori.adventure.text.Component;

import net.knightsandkings.knk.paper.utils.ColorOptions;

public class PlayerListener implements Listener {
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();

		e.joinMessage(Component.text("► " + "Player " + player.getName() + " joined").color(ColorOptions.message));
        
        player.sendMessage(Component.text("Welcome back " + player.getName()).color(ColorOptions.messageachievement));
		// if (!user.isNewUser()) {
		// 	user.sendMessage(ColorOptions.messageachievement + "Welcome back " + user.getName() + ", you have " + user.getCash() + " cash");
		// } else {
		// 	user.sendMessage(ColorOptions.messageachievement + "Welcome new player " + user.getName() + ", you have " + user.getCash() + " cash");
		// }

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
