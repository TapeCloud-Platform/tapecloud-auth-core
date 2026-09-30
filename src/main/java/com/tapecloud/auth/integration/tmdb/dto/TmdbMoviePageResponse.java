package com.tapecloud.auth.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMoviePageResponse(
        int page,
        List<TmdbMovieDto> results,
        @JsonProperty("total_pages") int totalPages
) {
}
