package com.tapecloud.auth.discovery;

import java.util.List;

/** Perfil de una entidad "autor" (artista en TapeBeat, persona en TapeFlix). */
public record DiscoveryProfile(
        String name,
        String imageUrl,
        String bio,
        String listeners,
        String playcount,
        List<String> tags,
        List<String> similar
) {
}
