package com.tapecloud.auth.discovery;

/** Resultado normalizado de cualquier proveedor externo (Last.fm, TMDb, etc.). */
public record DiscoveryItem(
        String externalId,
        String title,
        String subtitle,
        String description,
        String imageUrl,
        String genre,
        String kind
) {

    /** La mayoría de los resultados son contenido reproducible; solo los perfiles marcan otro kind. */
    public DiscoveryItem(
            String externalId,
            String title,
            String subtitle,
            String description,
            String imageUrl,
            String genre) {
        this(externalId, title, subtitle, description, imageUrl, genre, "content");
    }
}
