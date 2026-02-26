package net.knightsandkings.knk.core.ports.api;

import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.common.PagedQuery;
import net.knightsandkings.knk.core.domain.enchantments.KnkEnchantmentDefinition;

import java.util.concurrent.CompletableFuture;

public interface EnchantmentDefinitionsQueryApi {
    CompletableFuture<Page<KnkEnchantmentDefinition>> search(PagedQuery query);
    CompletableFuture<KnkEnchantmentDefinition> getById(int id);
}
