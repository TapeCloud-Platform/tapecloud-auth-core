package com.tapecloud.auth.discovery;

import java.util.List;

/** Filtro ofrecido por una app, con sus opciones sugeridas para poblar la UI. */
public record DiscoveryFilter(
        String type,
        String label,
        boolean freeText,
        List<DiscoveryFilterOption> options
) {
}
