package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmTrackInfoDto(
        String name,
        LastfmAlbumDto album
) {
}
