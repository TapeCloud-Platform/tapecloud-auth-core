package com.tapecloud.auth.integration.lastfm.dto;

import java.util.List;

public final class LastfmImages {

    // Hash de la imagen placeholder (estrella gris) que Last.fm devuelve cuando no hay portada.
    private static final String PLACEHOLDER_HASH = "2a96cbd8b46e442fc41c2b86b821562f";

    private LastfmImages() {
    }

    public static String largest(List<LastfmImageDto> images) {
        if (images == null) {
            return null;
        }
        return images.stream()
                .filter(img -> "extralarge".equals(img.size()))
                .map(LastfmImageDto::text)
                .filter(url -> url != null && !url.isBlank() && !url.contains(PLACEHOLDER_HASH))
                .findFirst()
                .orElse(null);
    }
}
