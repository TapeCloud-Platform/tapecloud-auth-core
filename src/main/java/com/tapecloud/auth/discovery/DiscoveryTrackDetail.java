package com.tapecloud.auth.discovery;

/** Info adicional de una canción (hoy solo el álbum de origen) para la ficha de detalle. */
public record DiscoveryTrackDetail(
        String albumName,
        String albumImageUrl,
        String albumDescription,
        int albumTrackCount
) {
}
