package net.knightsandkings.knk.api.impl.enchantment;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalEnchantmentRepositoryImplTest {

    private final LocalEnchantmentRepositoryImpl repository = new LocalEnchantmentRepositoryImpl();

    @Test
    void parsesKnownEnchantmentsFromLore() {
        List<String> lore = List.of("§7Poison II", "§7Flash Chaos I", "§8Some other lore");

        Map<String, Integer> result = repository.getEnchantments(lore).join();

        assertEquals(2, result.size());
        assertEquals(2, result.get("poison"));
        assertEquals(1, result.get("flash_chaos"));
    }

    @Test
    void applyReplacesExistingEnchantmentWithoutTouchingOtherLore() {
        List<String> lore = List.of("§8Flavor text", "§7Poison I");

        List<String> updated = repository.applyEnchantment(lore, "poison", 3).join();

        assertEquals(List.of("§8Flavor text", "§7Poison III"), updated);
    }

    @Test
    void removeDeletesOnlyTargetEnchantmentLine() {
        List<String> lore = List.of("§7Poison II", "§7Wither I", "§8Other");

        List<String> updated = repository.removeEnchantment(lore, "wither").join();

        assertEquals(List.of("§7Poison II", "§8Other"), updated);
    }

    @Test
    void hasChecksUseParsedResult() {
        List<String> lore = List.of("§7Health Boost I");

        assertTrue(repository.hasAnyEnchantment(lore).join());
        assertTrue(repository.hasEnchantment(lore, "health_boost").join());
        assertFalse(repository.hasEnchantment(lore, "poison").join());
    }
}
