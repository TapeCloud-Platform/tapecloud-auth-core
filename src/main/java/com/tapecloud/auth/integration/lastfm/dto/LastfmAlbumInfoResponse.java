package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmAlbumInfoResponse(
        Album album
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Album(String name, Tracks tracks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tracks(
            // Con un único tema Last.fm devuelve un objeto en lugar de una lista.
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<AlbumTrack> track
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlbumTrack(String name) {
    }

    /** Un single publicado como "álbum" trae una sola pista; los discos reales traen varias. */
    public int trackCount() {
        if (album == null || album.tracks() == null || album.tracks().track() == null) {
            return 0;
        }
        return album.tracks().track().size();
    }
}
