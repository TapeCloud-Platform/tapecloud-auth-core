package com.tapecloud.auth.discovery;

/** Resultado normalizado de cualquier proveedor externo (Last.fm, TMDb, etc.). */
public record DiscoveryItem(
        String externalId,
        String title,
        String subtitle,
        String description,
        String imageUrl,
        String genre
) {
}
