package com.tapecloud.auth.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbPersonSearchResponse(
        List<TmdbPersonDto> results
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbPersonDto(
            Long id,
            String name,
            @com.fasterxml.jackson.annotation.JsonProperty("profile_path") String profilePath,
            @com.fasterxml.jackson.annotation.JsonProperty("known_for_department") String knownForDepartment) {
    }
}
