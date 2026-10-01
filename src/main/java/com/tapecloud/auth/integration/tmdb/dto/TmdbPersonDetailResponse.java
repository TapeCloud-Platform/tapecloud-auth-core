package com.tapecloud.auth.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbPersonDetailResponse(
        Long id,
        String name,
        @com.fasterxml.jackson.annotation.JsonProperty("profile_path") String profilePath,
        @com.fasterxml.jackson.annotation.JsonProperty("known_for_department") String knownForDepartment,
        String biography,
        String birthday,
        Double popularity,
        @com.fasterxml.jackson.annotation.JsonProperty("imdb_id") String imdbId) {
}
