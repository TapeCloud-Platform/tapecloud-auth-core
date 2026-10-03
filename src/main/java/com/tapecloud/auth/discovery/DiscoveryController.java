package com.tapecloud.auth.discovery;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/discover")
public class DiscoveryController {

    private static final int MAX_LIMIT = 50;

    private final Map<String, DiscoveryProvider> providers;

    public DiscoveryController(List<DiscoveryProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(DiscoveryProvider::sourceApp, Function.identity()));
    }

    @GetMapping("/{sourceApp}/filters")
    public List<DiscoveryFilter> filters(@PathVariable String sourceApp) {
        return provider(sourceApp).availableFilters();
    }

    @GetMapping("/{sourceApp}")
    public List<DiscoveryItem> discover(
            @PathVariable String sourceApp,
            @RequestParam(defaultValue = "top") String type,
            @RequestParam(required = false) String value,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String album,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String people,
            @RequestParam(defaultValue = "30") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        if (genre != null || country != null || artist != null
                || album != null || search != null || people != null) {
            Map<String, String> filters = new java.util.LinkedHashMap<>();
            if (genre != null) {
                filters.put("genre", genre);
            }
            if (country != null) {
                filters.put("country", country);
            }
            if (artist != null) {
                filters.put("artist", artist);
            }
            if (album != null) {
                filters.put("album", album);
            }
            if (search != null) {
                filters.put("search", search);
            }
            if (people != null) {
                filters.put("people", people);
            }
            return provider(sourceApp).discoverCombined(filters, safeLimit);
        }
        return provider(sourceApp).discover(type, value, safeLimit);
    }

    @GetMapping("/{sourceApp}/profile")
    public DiscoveryProfile profile(@PathVariable String sourceApp, @RequestParam String value) {
        return provider(sourceApp).profile(value);
    }

    @GetMapping("/{sourceApp}/track")
    public DiscoveryTrackDetail trackDetail(
            @PathVariable String sourceApp,
            @RequestParam String artist,
            @RequestParam String track) {
        return provider(sourceApp).trackDetail(artist, track);
    }

    @GetMapping("/{sourceApp}/album")
    public DiscoveryTrackDetail albumDetail(
            @PathVariable String sourceApp,
            @RequestParam String artist,
            @RequestParam String album) {
        return provider(sourceApp).albumDetail(artist, album);
    }

    private DiscoveryProvider provider(String sourceApp) {
        DiscoveryProvider provider = providers.get(sourceApp);
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "App desconocida: " + sourceApp);
        }
        return provider;
    }
}
