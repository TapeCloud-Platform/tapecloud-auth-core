package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmAlbumSearchResponse(Results results) {

    public List<AlbumMatch> albumList() {
        if (results == null || results.albummatches() == null || results.albummatches().album() == null) {
            return List.of();
        }
        return results.albummatches().album();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Results(AlbumMatches albummatches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlbumMatches(List<AlbumMatch> album) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlbumMatch(
            String name,
            String artist,
            String url,
            List<LastfmImage> image,
            String listeners) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LastfmImage(String text, String size) {
    }
}
