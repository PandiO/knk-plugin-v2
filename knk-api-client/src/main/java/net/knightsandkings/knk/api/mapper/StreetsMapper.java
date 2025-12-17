package net.knightsandkings.knk.api.mapper;

import net.knightsandkings.knk.api.dto.*;
import net.knightsandkings.knk.core.domain.common.Page;
import net.knightsandkings.knk.core.domain.streets.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps Street DTOs from Web API to domain models.
 * READ-ONLY: no create/update DTOs.
 */
public class StreetsMapper {

    public StreetSummary toSummary(StreetListDto dto) {
        if (dto == null) {
            return null;
        }
        return new StreetSummary(
            dto.getId(),
            dto.getName()
        );
    }

    public StreetDetail toDetail(StreetDto dto) {
        if (dto == null) {
            return null;
        }
        return new StreetDetail(
            dto.getId(),
            dto.getName(),
            dto.getDistrictIds() != null ? dto.getDistrictIds() : Collections.emptyList(),
            dto.getDistricts() != null
                ? dto.getDistricts().stream().map(this::toDistrict).collect(Collectors.toList())
                : Collections.emptyList(),
            dto.getStructures() != null
                ? dto.getStructures().stream().map(this::toStructure).collect(Collectors.toList())
                : Collections.emptyList()
        );
    }

    public StreetDistrict toDistrict(StreetDistrictDto dto) {
        if (dto == null) {
            return null;
        }
        return new StreetDistrict(
            dto.getId(),
            dto.getName(),
            dto.getDescription(),
            dto.getAllowEntry(),
            dto.getAllowExit(),
            dto.getWgRegionId()
        );
    }

    public StreetStructure toStructure(StreetStructureDto dto) {
        if (dto == null) {
            return null;
        }
        return new StreetStructure(
            dto.getId(),
            dto.getName(),
            dto.getDescription(),
            dto.getHouseNumber(),
            dto.getDistrictId()
        );
    }

    public Page<StreetSummary> toPage(StreetListDtoPagedResultDto dto) {
        if (dto == null) {
            return new Page<>(Collections.emptyList(), 0, 1, 10);
        }
        List<StreetSummary> items = dto.getItems() != null
            ? dto.getItems().stream().map(this::toSummary).collect(Collectors.toList())
            : Collections.emptyList();
        return new Page<>(
            items,
            dto.getTotalCount() != null ? dto.getTotalCount() : 0,
            dto.getPageNumber() != null ? dto.getPageNumber() : 1,
            dto.getPageSize() != null ? dto.getPageSize() : 10
        );
    }
}
