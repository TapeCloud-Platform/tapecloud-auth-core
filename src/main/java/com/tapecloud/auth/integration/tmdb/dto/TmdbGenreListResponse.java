package com.tapecloud.auth.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenreListResponse(
        List<TmdbGenreDto> genres
) {
}
