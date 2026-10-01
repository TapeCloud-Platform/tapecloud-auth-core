package com.tapecloud.auth.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbPersonCreditsResponse(List<TmdbCredit> cast) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbCredit(
            Long id,
            String title,
            Double popularity,
            @com.fasterxml.jackson.annotation.JsonProperty("poster_path") String posterPath) {
    }
}
