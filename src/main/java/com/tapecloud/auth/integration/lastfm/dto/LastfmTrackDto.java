package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmTrackDto(
        String name,
        String mbid,
        String url,
        String playcount,
        String listeners,
        LastfmArtistRefDto artist,
        List<LastfmImageDto> image
) {

    public String imageUrl() {
        return LastfmImages.largest(image);
    }
}
