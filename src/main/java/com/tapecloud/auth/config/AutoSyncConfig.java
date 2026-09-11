package com.tapecloud.auth.config;

import com.tapecloud.auth.integration.tmdb.TmdbSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoSyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AutoSyncConfig.class);

    @Bean
    public CommandLineRunner autoSyncMovies(TmdbSyncService tmdbSyncService) {
        return args -> {
            try {
                // Sincroniza 10 páginas de Animation (~200-250 películas/shows)
                // Con 10 requests estamos bien bajo el límite de 40 req/10seg de TMDB
                for (int page = 1; page <= 10; page++) {
                    tmdbSyncService.syncPopularMovies(page);
                    // Pequeño delay para respetar rate limits
                    Thread.sleep(250);
                }
                log.info("Sincronización automática de películas Animation completada al iniciar el servidor.");
            } catch (Exception e) {
                log.warn("No se pudo realizar la sincronización automática de TMDB al iniciar: {}", e.getMessage());
            }
        };
    }
}
