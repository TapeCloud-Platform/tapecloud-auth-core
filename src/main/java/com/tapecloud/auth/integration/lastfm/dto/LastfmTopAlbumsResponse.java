package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmTopAlbumsResponse(
        TopAlbums topalbums
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopAlbums(List<AlbumEntry> album) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlbumEntry(
            String name,
            String mbid,
            String playcount,
            LastfmArtistRefDto artist,
            List<LastfmImageDto> image
    ) {
    }
}
