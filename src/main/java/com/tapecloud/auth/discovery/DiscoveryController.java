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
            @RequestParam(defaultValue = "30") int limit) {
        return provider(sourceApp).discover(type, value, Math.min(Math.max(limit, 1), MAX_LIMIT));
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
