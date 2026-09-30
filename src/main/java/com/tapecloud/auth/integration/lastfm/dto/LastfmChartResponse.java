package com.tapecloud.auth.integration.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** artist.getTopTracks responde bajo "toptracks"; el resto de los métodos usa "tracks". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LastfmChartResponse(
        LastfmTracksDto tracks,
        LastfmTracksDto toptracks
) {

    public List<LastfmTrackDto> trackList() {
        LastfmTracksDto container = tracks != null ? tracks : toptracks;
        if (container == null || container.track() == null) {
            return List.of();
        }
        return container.track();
    }
}
