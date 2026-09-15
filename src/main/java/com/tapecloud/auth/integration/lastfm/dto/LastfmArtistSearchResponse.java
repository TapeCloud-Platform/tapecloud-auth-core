package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmArtistSearchResponse(
        Results results
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Results(ArtistMatches artistmatches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistMatches(List<ArtistMatch> artist) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArtistMatch(
            String name,
            String mbid,
            String listeners,
            List<LastfmImageDto> image
    ) {
    }

    public List<ArtistMatch> artistList() {
        if (results == null || results.artistmatches() == null || results.artistmatches().artist() == null) {
            return List.of();
        }
        return results.artistmatches().artist();
    }
}
