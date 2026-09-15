package com.tapecloud.auth.discovery;

import java.util.List;

/**
 * Contrato común de exploración por filtros. Cada app (tapebeat, tapeflix) lo implementa
 * sobre su propia API externa manteniendo los mismos tipos de filtro.
 */
public interface DiscoveryProvider {

    String sourceApp();

    List<DiscoveryFilter> availableFilters();

    List<DiscoveryItem> discover(String type, String value, int limit);
}
