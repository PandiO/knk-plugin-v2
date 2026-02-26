package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.material.KnkMinecraftMaterialRef;

import java.util.concurrent.CompletableFuture;

public interface MinecraftMaterialRefsQueryApi {
    CompletableFuture<KnkMinecraftMaterialRef> getById(int id);
}
