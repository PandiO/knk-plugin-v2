package net.knightsandkings.knk.core.domain.streets;

/**
 * Minimal street summary for list views.
 * Corresponds to StreetListDto from Web API.
 */
public record StreetSummary(
    Integer id,
    String name
) {}
