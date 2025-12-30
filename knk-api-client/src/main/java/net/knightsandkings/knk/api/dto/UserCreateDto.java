package net.knightsandkings.knk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.UUID;
import java.util.Date;

/**
 * DTO for creating a user.
 * Minimal fields inferred from existing DTOs; backend may accept fewer.
 * TODO: Align with swagger contract (required/optional fields).
 */
public record UserCreateDto(
    @JsonProperty("username") String username,
    @JsonProperty("uuid") UUID uuid,
    @JsonProperty("email") String email,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @JsonProperty("createdAt") Date createdAt
) {}
