package net.knightsandkings.knk.paper.commands.enchantment;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface EnchantmentSubcommand {
    String name();

    String permission();

    boolean execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
