package net.knightsandkings.knk.core.ports.enchantment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EnchantmentRepository {
    CompletableFuture<Map<String, Integer>> getEnchantments(List<String> loreLines);
    CompletableFuture<Boolean> hasAnyEnchantment(List<String> loreLines);
    CompletableFuture<Boolean> hasEnchantment(List<String> loreLines, String enchantmentId);
    CompletableFuture<List<String>> applyEnchantment(List<String> loreLines, String enchantmentId, Integer level);
    CompletableFuture<List<String>> removeEnchantment(List<String> loreLines, String enchantmentId);
}
