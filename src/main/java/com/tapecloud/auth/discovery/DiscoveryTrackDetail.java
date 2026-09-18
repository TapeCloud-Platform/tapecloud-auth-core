package com.tapecloud.auth.discovery;

import java.util.List;

/** Info adicional de una canción (hoy solo el álbum de origen) para la ficha de detalle. */
public record DiscoveryTrackDetail(
        String albumName,
        String albumImageUrl,
        String albumDescription,
        int albumTrackCount,
        List<String> albumTracks,
        int durationSeconds
) {
}
