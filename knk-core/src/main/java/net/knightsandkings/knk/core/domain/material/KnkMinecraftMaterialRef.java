package net.knightsandkings.knk.core.domain.material;

public record KnkMinecraftMaterialRef(
        Integer id,
        String namespaceKey,
        String legacyName,
        String category,
        String iconUrl
) {}
