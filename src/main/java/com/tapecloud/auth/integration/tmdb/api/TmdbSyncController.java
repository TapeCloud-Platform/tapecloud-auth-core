package com.tapecloud.auth.integration.tmdb.api;

import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.integration.tmdb.TmdbSyncService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/sync/tmdb")
public class TmdbSyncController {

    private final TmdbSyncService tmdbSyncService;

    public TmdbSyncController(TmdbSyncService tmdbSyncService) {
        this.tmdbSyncService = tmdbSyncService;
    }

    @PostMapping("/movies")
    public List<ContentItem> syncPopularMovies(@RequestParam(defaultValue = "1") int page) {
        return tmdbSyncService.syncPopularMovies(page);
    }
}
