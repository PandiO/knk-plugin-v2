package net.knightsandkings.knk.api.impl.enchantment;

import net.knightsandkings.knk.core.domain.enchantment.Enchantment;
import net.knightsandkings.knk.core.domain.enchantment.EnchantmentRegistry;
import net.knightsandkings.knk.core.ports.enchantment.EnchantmentRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

public class LocalEnchantmentRepositoryImpl implements EnchantmentRepository {
    private static final String PREFIX = "§7";
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    @Override
    public CompletableFuture<Map<String, Integer>> getEnchantments(List<String> loreLines) {
        return CompletableFuture.completedFuture(parseEnchantments(loreLines));
    }

    @Override
    public CompletableFuture<Boolean> hasAnyEnchantment(List<String> loreLines) {
        return CompletableFuture.completedFuture(!parseEnchantments(loreLines).isEmpty());
    }

    @Override
    public CompletableFuture<Boolean> hasEnchantment(List<String> loreLines, String enchantmentId) {
        String normalizedId = normalizeId(enchantmentId);
        return CompletableFuture.completedFuture(parseEnchantments(loreLines).containsKey(normalizedId));
    }

    @Override
    public CompletableFuture<List<String>> applyEnchantment(List<String> loreLines, String enchantmentId, Integer level) {
        EnchantmentRegistry registry = EnchantmentRegistry.getInstance();
        String normalizedId = normalizeId(enchantmentId);
        if (!registry.existsAndValidLevel(normalizedId, level)) {
            return CompletableFuture.completedFuture(copyLore(loreLines));
        }

        List<String> updatedLore = new ArrayList<>();
        List<String> sourceLore = copyLore(loreLines);
        boolean replaced = false;

        for (String line : sourceLore) {
            ParsedLoreLine parsed = parseLoreLine(line);
            if (parsed != null && normalizedId.equals(parsed.enchantmentId())) {
                Enchantment enchantment = registry.getById(normalizedId).orElseThrow();
                updatedLore.add(formatLore(enchantment.displayName(), level));
                replaced = true;
            } else {
                updatedLore.add(line);
            }
        }

        if (!replaced) {
            Enchantment enchantment = registry.getById(normalizedId).orElseThrow();
            updatedLore.add(formatLore(enchantment.displayName(), level));
        }

        return CompletableFuture.completedFuture(updatedLore);
    }

    @Override
    public CompletableFuture<List<String>> removeEnchantment(List<String> loreLines, String enchantmentId) {
        String normalizedId = normalizeId(enchantmentId);
        List<String> sourceLore = copyLore(loreLines);
        List<String> updatedLore = new ArrayList<>();

        for (String line : sourceLore) {
            ParsedLoreLine parsed = parseLoreLine(line);
            if (parsed == null || !normalizedId.equals(parsed.enchantmentId())) {
                updatedLore.add(line);
            }
        }

        return CompletableFuture.completedFuture(updatedLore);
    }

    private Map<String, Integer> parseEnchantments(List<String> loreLines) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String line : copyLore(loreLines)) {
            ParsedLoreLine parsed = parseLoreLine(line);
            if (parsed != null) {
                result.put(parsed.enchantmentId(), parsed.level());
            }
        }
        return result;
    }

    private ParsedLoreLine parseLoreLine(String loreLine) {
        if (loreLine == null || loreLine.isBlank()) {
            return null;
        }

        String plain = stripColor(loreLine).trim();
        int lastSpace = plain.lastIndexOf(' ');
        if (lastSpace <= 0 || lastSpace >= plain.length() - 1) {
            return null;
        }

        String namePart = plain.substring(0, lastSpace).trim();
        String levelPart = plain.substring(lastSpace + 1).trim();
        int level = stringToLevel(levelPart);
        if (level < 1) {
            return null;
        }

        Optional<Enchantment> enchantment = EnchantmentRegistry.getInstance()
                .getAll()
                .stream()
                .filter(e -> e.displayName().equalsIgnoreCase(namePart))
                .findFirst();

        if (enchantment.isEmpty()) {
            return null;
        }

        return new ParsedLoreLine(enchantment.get().id(), level);
    }

    private String formatLore(String displayName, int level) {
        return PREFIX + displayName + " " + levelToString(level);
    }

    private String stripColor(String value) {
        return COLOR_PATTERN.matcher(value).replaceAll("");
    }

    private int stringToLevel(String value) {
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            default -> -1;
        };
    }

    private String levelToString(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> Integer.toString(value);
        };
    }

    private String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> copyLore(List<String> loreLines) {
        return loreLines == null ? new ArrayList<>() : new ArrayList<>(loreLines);
    }

    private record ParsedLoreLine(String enchantmentId, int level) {}
}
