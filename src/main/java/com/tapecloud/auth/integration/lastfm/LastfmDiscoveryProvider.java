package com.tapecloud.auth.integration.lastfm;

import com.tapecloud.auth.discovery.DiscoveryFilter;
import com.tapecloud.auth.discovery.DiscoveryFilterOption;
import com.tapecloud.auth.discovery.DiscoveryItem;
import com.tapecloud.auth.discovery.DiscoveryProvider;
import com.tapecloud.auth.integration.lastfm.dto.LastfmImages;
import com.tapecloud.auth.integration.lastfm.dto.LastfmSearchResponse;
import com.tapecloud.auth.integration.lastfm.dto.LastfmTrackDto;
import com.tapecloud.auth.integration.lastfm.dto.LastfmTrackInfoResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LastfmDiscoveryProvider implements DiscoveryProvider {

    private static final List<DiscoveryFilterOption> GENRES = List.of(
            new DiscoveryFilterOption("rock", "Rock"),
            new DiscoveryFilterOption("pop", "Pop"),
            new DiscoveryFilterOption("electronic", "Electrónica"),
            new DiscoveryFilterOption("hip-hop", "Hip-Hop"),
            new DiscoveryFilterOption("jazz", "Jazz"),
            new DiscoveryFilterOption("metal", "Metal"),
            new DiscoveryFilterOption("indie", "Indie"),
            new DiscoveryFilterOption("shoegaze", "Shoegaze"),
            new DiscoveryFilterOption("punk", "Punk"),
            new DiscoveryFilterOption("folk", "Folk"),
            new DiscoveryFilterOption("ambient", "Ambient"),
            new DiscoveryFilterOption("bossa nova", "Bossa Nova"),
            new DiscoveryFilterOption("reggaeton", "Reggaetón"),
            new DiscoveryFilterOption("rock argentino", "Rock Argentino"),
            new DiscoveryFilterOption("cumbia", "Cumbia"),
            new DiscoveryFilterOption("vaporwave", "Vaporwave"),
            new DiscoveryFilterOption("math rock", "Math Rock"),
            new DiscoveryFilterOption("city pop", "City Pop")
    );

    private static final List<DiscoveryFilterOption> COUNTRIES = List.of(
            new DiscoveryFilterOption("argentina", "Argentina"),
            new DiscoveryFilterOption("brazil", "Brasil"),
            new DiscoveryFilterOption("spain", "España"),
            new DiscoveryFilterOption("mexico", "México"),
            new DiscoveryFilterOption("united states", "Estados Unidos"),
            new DiscoveryFilterOption("united kingdom", "Reino Unido"),
            new DiscoveryFilterOption("japan", "Japón"),
            new DiscoveryFilterOption("germany", "Alemania"),
            new DiscoveryFilterOption("france", "Francia")
    );

    private final LastfmClient lastfmClient;

    public LastfmDiscoveryProvider(LastfmClient lastfmClient) {
        this.lastfmClient = lastfmClient;
    }

    @Override
    public String sourceApp() {
        return "tapebeat";
    }

    @Override
    public List<DiscoveryFilter> availableFilters() {
        return List.of(
                new DiscoveryFilter("top", "Más escuchadas", false, List.of()),
                new DiscoveryFilter("genre", "Género", true, GENRES),
                new DiscoveryFilter("country", "País", false, COUNTRIES),
                new DiscoveryFilter("artist", "Artista", true, List.of()),
                new DiscoveryFilter("search", "Buscar canción", true, List.of())
        );
    }

    @Override
    public List<DiscoveryItem> discover(String type, String value, int limit) {
        if ("search".equals(type)) {
            return searchItems(requireValue(value, "búsqueda"), limit);
        }

        List<LastfmTrackDto> tracks = switch (type) {
            case "top" -> lastfmClient.fetchTopTracks(1, limit).tracks().track();
            case "genre" -> lastfmClient.fetchTracksByTag(requireValue(value, "género"), limit).tracks().track();
            case "country" -> lastfmClient.fetchTracksByCountry(requireValue(value, "país"), limit).tracks().track();
            case "artist" -> lastfmClient.fetchTracksByArtist(requireValue(value, "artista"), limit).tracks().track();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro no soportado: " + type);
        };

        return enrich(tracks);
    }

    private List<DiscoveryItem> searchItems(String query, int limit) {
        LastfmSearchResponse response = lastfmClient.searchTracks(query, limit);
        if (response == null || response.results() == null || response.results().trackmatches() == null) {
            return List.of();
        }
        List<LastfmSearchResponse.SearchTrack> matches = response.results().trackmatches().track();
        if (matches == null) {
            return List.of();
        }

        return parallelMap(matches, track -> new DiscoveryItem(
                externalId(track.mbid(), track.artist(), track.name()),
                track.name(),
                track.artist(),
                formatListeners(track.listeners(), track.artist()),
                resolveImage(LastfmImages.largest(track.image()), track.artist(), track.name()),
                track.artist()
        ));
    }

    private List<DiscoveryItem> enrich(List<LastfmTrackDto> tracks) {
        if (tracks == null) {
            return List.of();
        }

        return parallelMap(tracks, track -> {
            String artist = track.artist() != null ? track.artist().name() : "Artista desconocido";
            return new DiscoveryItem(
                    externalId(track.mbid(), artist, track.name()),
                    track.name(),
                    artist,
                    buildDescription(track, artist),
                    resolveImage(track.imageUrl(), artist, track.name()),
                    artist
            );
        });
    }

    /** Cada portada requiere un track.getInfo extra, así que se resuelven en paralelo acotado. */
    private <T> List<DiscoveryItem> parallelMap(List<T> source, java.util.function.Function<T, DiscoveryItem> mapper) {
        try (ExecutorService executor = Executors.newFixedThreadPool(6)) {
            List<Future<DiscoveryItem>> futures = source.stream()
                    .map(item -> executor.submit(() -> mapper.apply(item)))
                    .toList();

            return futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }).filter(java.util.Objects::nonNull).toList();
        }
    }

    private String resolveImage(String candidate, String artist, String trackName) {
        if (candidate != null) {
            return candidate;
        }
        try {
            LastfmTrackInfoResponse info = lastfmClient.fetchTrackInfo(artist, trackName);
            if (info != null && info.track() != null && info.track().album() != null) {
                return LastfmImages.largest(info.track().album().image());
            }
        } catch (Exception ignored) {
            // Sin portada disponible.
        }
        return null;
    }

    private String externalId(String mbid, String artist, String name) {
        return mbid != null && !mbid.isBlank() ? mbid : artist + "-" + name;
    }

    private String buildDescription(LastfmTrackDto track, String artist) {
        String listeners = track.listeners() != null && !track.listeners().isBlank() ? track.listeners() : null;
        String playcount = track.playcount() != null && !track.playcount().isBlank() ? track.playcount() : null;
        if (listeners == null && playcount == null) {
            return artist;
        }
        return "%s · %s oyentes · %s reproducciones".formatted(artist, orZero(listeners), orZero(playcount));
    }

    private String formatListeners(String listeners, String artist) {
        return listeners == null || listeners.isBlank() ? artist : "%s · %s oyentes".formatted(artist, listeners);
    }

    private String orZero(String value) {
        return value == null ? "0" : value;
    }

    private String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el valor de " + label);
        }
        return value;
    }
}
