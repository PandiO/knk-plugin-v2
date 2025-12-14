package net.knightsandkings.knk.core.domain.common;

import java.util.Map;

public record PagedQuery(int pageNumber, int pageSize, String searchTerm, String sortBy, boolean sortDescending, Map<String, String> filters) {
}
