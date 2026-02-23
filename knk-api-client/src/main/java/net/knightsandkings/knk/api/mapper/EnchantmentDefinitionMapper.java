package net.knightsandkings.knk.api.mapper;

import net.knightsandkings.knk.api.dto.EnchantmentDefinitionDto;
import net.knightsandkings.knk.api.dto.EnchantmentDefinitionListDto;
import net.knightsandkings.knk.api.dto.EnchantmentDefinitionListDtoPagedResultDto;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.enchantments.KnkEnchantmentDefinition;

import java.util.Collections;
import java.util.List;

public final class EnchantmentDefinitionMapper {
    private EnchantmentDefinitionMapper() {}

    public static KnkEnchantmentDefinition toCore(EnchantmentDefinitionDto dto) {
        if (dto == null) return null;

        String baseNamespaceKey = dto.baseEnchantmentRef() != null
                ? dto.baseEnchantmentRef().namespaceKey()
                : null;

        return new KnkEnchantmentDefinition(
                dto.id(),
                dto.key(),
                dto.displayName(),
                dto.description(),
                dto.isCustom(),
                dto.maxLevel(),
                dto.minecraftEnchantmentRefId(),
                baseNamespaceKey
        );
    }

    public static KnkEnchantmentDefinition toCore(EnchantmentDefinitionListDto dto) {
        if (dto == null) return null;

        return new KnkEnchantmentDefinition(
                dto.id(),
                dto.key(),
                dto.displayName(),
                dto.description(),
                dto.isCustom(),
                dto.maxLevel(),
                dto.minecraftEnchantmentRefId(),
                dto.baseEnchantmentNamespaceKey()
        );
    }

    public static Page<KnkEnchantmentDefinition> mapPagedList(EnchantmentDefinitionListDtoPagedResultDto result) {
        if (result == null) {
            return new Page<>(Collections.emptyList(), 0, 1, 10);
        }

        List<KnkEnchantmentDefinition> items = result.items() != null
                ? result.items().stream().map(EnchantmentDefinitionMapper::toCore).toList()
                : Collections.emptyList();

        int totalCount = result.totalItems() != null ? Math.toIntExact(result.totalItems()) : items.size();
        int pageNumber = result.pageNumber() != null ? result.pageNumber() : 1;
        int pageSize = result.pageSize() != null ? result.pageSize() : Math.max(items.size(), 1);

        return new Page<>(items, totalCount, pageNumber, pageSize);
    }
}
