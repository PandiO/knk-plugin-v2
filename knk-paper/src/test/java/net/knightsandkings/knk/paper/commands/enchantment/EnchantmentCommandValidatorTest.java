package net.knightsandkings.knk.paper.commands.enchantment;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentCommandValidatorTest {
    private final EnchantmentCommandValidator validator = new EnchantmentCommandValidator();

    @Test
    void normalizeEnchantmentIdConvertsSpacesAndDashes() {
        String normalized = validator.normalizeEnchantmentId(" Flash-Chaos ");
        assertEquals("flash_chaos", normalized);
    }

    @Test
    void parsePositiveLevelRejectsInvalidValues() {
        assertTrue(validator.parsePositiveLevel("2").isPresent());
        assertFalse(validator.parsePositiveLevel("0").isPresent());
        assertFalse(validator.parsePositiveLevel("abc").isPresent());
    }

    @Test
    void resolveEnchantmentUsesRegistryIds() {
        assertTrue(validator.resolveEnchantment("poison").isPresent());
        assertTrue(validator.resolveEnchantment("flash chaos").isPresent());
        assertFalse(validator.resolveEnchantment("does_not_exist").isPresent());
    }

    @Test
    void hasEnchantmentPermissionUsesCustomNodePrefix() {
        CommandSender sender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class[]{CommandSender.class},
                (proxy, method, args) -> {
                    if ("hasPermission".equals(method.getName()) && args != null && args.length == 1) {
                        return "customenchantments.flash_chaos".equals(String.valueOf(args[0]));
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        assertTrue(validator.hasEnchantmentPermission(sender, "flash-chaos"));
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
}
