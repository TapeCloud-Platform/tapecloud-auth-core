package com.tapecloud.auth.integration.tmdb.api;

import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.integration.tmdb.TmdbSyncService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/sync/tmdb")
public class TmdbSyncController {

    private static final Logger log = LoggerFactory.getLogger(TmdbSyncController.class);
    private static final int MAX_PAGES_PER_REQUEST = 10;

    private final TmdbSyncService tmdbSyncService;

    public TmdbSyncController(TmdbSyncService tmdbSyncService) {
        this.tmdbSyncService = tmdbSyncService;
    }

    @PostMapping("/movies")
    public List<ContentItem> syncPopularMovies(@RequestParam(defaultValue = "1") int page) {
        return tmdbSyncService.syncPopularMovies(page);
    }

    @PostMapping("/movies/bulk")
    public List<ContentItem> syncBulkMovies(
            @RequestParam(defaultValue = "1") int startPage,
            @RequestParam(defaultValue = "5") int pageCount) {
        
        // Limitar máximo de páginas por seguridad (evitar saturar API de TMDB)
        int safePageCount = Math.min(pageCount, MAX_PAGES_PER_REQUEST);
        log.info("Iniciando sincronización bulk: startPage={}, pageCount={}", startPage, safePageCount);
        
        List<ContentItem> allItems = new ArrayList<>();
        for (int i = 0; i < safePageCount; i++) {
            try {
                List<ContentItem> pageItems = tmdbSyncService.syncPopularMovies(startPage + i);
                allItems.addAll(pageItems);
                // Delay entre requests para respetar rate limits de TMDB
                Thread.sleep(250);
            } catch (InterruptedException e) {
                log.warn("Sincronización bulk interrumpida: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("Sincronización bulk completada: {} películas sincronizadas", allItems.size());
        return allItems;
    }
}
