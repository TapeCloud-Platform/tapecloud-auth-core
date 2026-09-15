package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmSearchResponse(
        Results results
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Results(TrackMatches trackmatches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrackMatches(List<SearchTrack> track) {
    }

    /** En track.search el artista viene como string plano, no como objeto. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchTrack(
            String name,
            String artist,
            String mbid,
            String listeners,
            List<LastfmImageDto> image
    ) {
    }
}
