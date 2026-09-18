package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmTrackInfoDto(
        String name,
        String duration,
        LastfmAlbumDto album
) {

    /** "duration" viene en milisegundos, como string, y puede faltar (0 o vacío). */
    public int durationSeconds() {
        if (duration == null || duration.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(duration.trim()) / 1000;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
