package com.tapecloud.auth.integration.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MusicbrainzArtistSearchResponse(
        List<MusicbrainzArtist> artists
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MusicbrainzArtist(String id, String name, int score) {
    }
}
