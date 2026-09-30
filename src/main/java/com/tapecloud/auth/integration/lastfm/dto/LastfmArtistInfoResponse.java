package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmArtistInfoResponse(
        Artist artist
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artist(
            String name,
            String mbid,
            String url,
            Stats stats,
            Tags tags,
            Similar similar,
            Bio bio
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stats(String listeners, String playcount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tags(List<Tag> tag) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tag(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Similar(List<SimilarArtist> artist) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SimilarArtist(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bio(String summary, String content) {
    }
}
