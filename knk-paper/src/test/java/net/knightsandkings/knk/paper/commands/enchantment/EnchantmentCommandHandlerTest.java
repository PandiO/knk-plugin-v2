package net.knightsandkings.knk.paper.commands.enchantment;

import net.knightsandkings.knk.core.ports.enchantment.CooldownManager;
import net.knightsandkings.knk.core.ports.enchantment.EnchantmentRepository;
import net.knightsandkings.knk.paper.config.EnchantmentConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentCommandHandlerTest {

    @Test
    void tabCompleteProvidesRootCommands() {
        EnchantmentCommandHandler handler = createHandler();
        List<String> completions = handler.onTabComplete(testSender(), null, "ce", new String[]{""});

        assertTrue(completions.contains("add"));
        assertTrue(completions.contains("remove"));
        assertTrue(completions.contains("info"));
        assertTrue(completions.contains("cooldown"));
        assertTrue(completions.contains("reload"));
    }

    @Test
    void tabCompleteProvidesCooldownClear() {
        EnchantmentCommandHandler handler = createHandler();
        List<String> completions = handler.onTabComplete(testSender(), null, "ce", new String[]{"cooldown", ""});

        assertTrue(completions.contains("clear"));
    }

    private EnchantmentCommandHandler createHandler() {
        Plugin plugin = (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class[]{Plugin.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );

        EnchantmentConfigManager configManager = new EnchantmentConfigManager(plugin);

        return new EnchantmentCommandHandler(
            plugin,
            configManager,
            new NoOpEnchantmentRepository(),
            new NoOpCooldownManager()
        );
    }

    private CommandSender testSender() {
        return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class[]{CommandSender.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0f;
        }
        if (returnType == double.class) {
            return 0.0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class NoOpEnchantmentRepository implements EnchantmentRepository {
        @Override
        public CompletableFuture<Map<String, Integer>> getEnchantments(List<String> loreLines) {
            return CompletableFuture.completedFuture(Map.of());
        }

        @Override
        public CompletableFuture<Boolean> hasAnyEnchantment(List<String> loreLines) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<Boolean> hasEnchantment(List<String> loreLines, String enchantmentId) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<List<String>> applyEnchantment(List<String> loreLines, String enchantmentId, Integer level) {
            return CompletableFuture.completedFuture(loreLines);
        }

        @Override
        public CompletableFuture<List<String>> removeEnchantment(List<String> loreLines, String enchantmentId) {
            return CompletableFuture.completedFuture(loreLines);
        }
    }

    private static final class NoOpCooldownManager implements CooldownManager {
        @Override
        public CompletableFuture<Long> getRemainingCooldown(UUID playerId, String enchantmentId) {
            return CompletableFuture.completedFuture(0L);
        }

        @Override
        public CompletableFuture<Void> applyCooldown(UUID playerId, String enchantmentId, long durationMs) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> clearCooldowns(UUID playerId) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
