package net.knightsandkings.knk.core.domain.common;

import java.util.List;

public record Page<T>(List<T> items, int totalCount, int pageNumber, int pageSize) {
}
