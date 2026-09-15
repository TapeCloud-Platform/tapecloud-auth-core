package com.tapecloud.auth.integration.lastfm;

import com.tapecloud.auth.discovery.DiscoveryFilter;
import com.tapecloud.auth.discovery.DiscoveryFilterOption;
import com.tapecloud.auth.discovery.DiscoveryItem;
import com.tapecloud.auth.discovery.DiscoveryProfile;
import com.tapecloud.auth.discovery.DiscoveryProvider;
import com.tapecloud.auth.integration.lastfm.dto.LastfmAlbumInfoResponse;
import com.tapecloud.auth.integration.lastfm.dto.LastfmArtistInfoResponse;
import com.tapecloud.auth.integration.lastfm.dto.LastfmArtistSearchResponse;
import com.tapecloud.auth.integration.lastfm.dto.LastfmImages;
import com.tapecloud.auth.integration.lastfm.dto.LastfmSearchResponse;
import com.tapecloud.auth.integration.lastfm.dto.LastfmTopAlbumsResponse;
import com.tapecloud.auth.integration.lastfm.dto.LastfmTrackDto;
import com.tapecloud.auth.integration.lastfm.dto.LastfmTrackInfoResponse;
import com.tapecloud.auth.integration.musicbrainz.MusicbrainzDiscographyService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LastfmDiscoveryProvider implements DiscoveryProvider {

    private static final int ARTIST_MATCH_LIMIT = 6;
    private static final int ARTIST_SEARCH_POOL = 20;

    // Cuántos títulos se verifican contra album.getInfo antes de recortar al límite pedido.
    private static final int DISCOGRAPHY_POOL = 30;
    private static final int MIN_ALBUM_TRACKS = 4;

    // Last.fm corta las ráfagas: la verificación de discos usa menos hilos que el resto.
    private static final int DEFAULT_THREADS = 6;
    private static final int ALBUM_INFO_THREADS = 2;

    // Last.fm indexa perfiles duplicados y vacíos; por debajo de este umbral son ruido.
    private static final long MIN_ARTIST_LISTENERS = 5_000L;

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
    private final MusicbrainzDiscographyService musicbrainzDiscographyService;

    public LastfmDiscoveryProvider(
            LastfmClient lastfmClient, MusicbrainzDiscographyService musicbrainzDiscographyService) {
        this.lastfmClient = lastfmClient;
        this.musicbrainzDiscographyService = musicbrainzDiscographyService;
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
            return unifiedSearch(requireValue(value, "búsqueda"), limit);
        }
        if ("discography".equals(type)) {
            return discographyItems(requireValue(value, "artista"), limit);
        }

        List<LastfmTrackDto> tracks = switch (type) {
            case "top" -> lastfmClient.fetchTopTracks(1, limit).trackList();
            case "genre" -> lastfmClient.fetchTracksByTag(requireValue(value, "género"), limit).trackList();
            case "country" -> lastfmClient.fetchTracksByCountry(requireValue(value, "país"), limit).trackList();
            case "artist" -> lastfmClient.fetchTracksByArtist(requireValue(value, "artista"), limit).trackList();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro no soportado: " + type);
        };

        return enrich(tracks);
    }

    /**
     * La lupa debe encontrar artistas aunque el texto sea parcial o tenga erratas ("Soda" →
     * Soda Stereo). Los perfiles encontrados encabezan el resultado y las canciones van después.
     */
    private List<DiscoveryItem> unifiedSearch(String query, int limit) {
        List<DiscoveryItem> artists = artistMatches(query, ARTIST_MATCH_LIMIT);
        List<DiscoveryItem> tracks = trackMatches(query, limit);

        return java.util.stream.Stream.concat(artists.stream(), tracks.stream()).toList();
    }

    private List<DiscoveryItem> trackMatches(String query, int limit) {
        try {
            List<LastfmTrackDto> byArtist = lastfmClient.fetchTracksByArtist(query, limit).trackList();
            if (!byArtist.isEmpty()) {
                return enrich(byArtist);
            }
        } catch (Exception ignored) {
            // El artista no existe; se intenta por título.
        }

        return searchItems(query, limit);
    }

    /** Last.fm ordena por relevancia textual, pero conviene priorizar el match exacto y el popular. */
    private List<DiscoveryItem> artistMatches(String query, int limit) {
        List<LastfmArtistSearchResponse.ArtistMatch> matches;
        try {
            matches = lastfmClient.searchArtists(query, ARTIST_SEARCH_POOL).artistList();
        } catch (Exception e) {
            return List.of();
        }

        String normalized = query.trim().toLowerCase(java.util.Locale.ROOT);
        List<LastfmArtistSearchResponse.ArtistMatch> relevant = matches.stream()
                .filter(match -> match.name() != null && !match.name().isBlank())
                .filter(match -> listenersOf(match.listeners()) >= MIN_ARTIST_LISTENERS)
                .sorted(java.util.Comparator
                        .comparingInt((LastfmArtistSearchResponse.ArtistMatch m) -> relevance(m.name(), normalized))
                        .thenComparing(m -> -listenersOf(m.listeners())))
                .limit(limit)
                .toList();

        return parallelMap(relevant, match -> new DiscoveryItem(
                "artist:" + match.name(),
                match.name(),
                "Artista",
                "%s oyentes".formatted(orZero(match.listeners())),
                artistImage(match.name()),
                match.name(),
                "artist"
        ));
    }

    private static int relevance(String name, String query) {
        String candidate = name.toLowerCase(java.util.Locale.ROOT);
        if (candidate.equals(query)) {
            return 0;
        }
        if (candidate.startsWith(query)) {
            return 1;
        }
        return candidate.contains(query) ? 2 : 3;
    }

    /** Last.fm dejó de publicar fotos de artista, así que se usa la portada de su disco principal. */
    private String artistImage(String artist) {
        try {
            LastfmTopAlbumsResponse albums = lastfmClient.fetchArtistAlbums(artist, 3);
            if (albums == null || albums.topalbums() == null || albums.topalbums().album() == null) {
                return null;
            }
            return albums.topalbums().album().stream()
                    .map(album -> LastfmImages.largest(album.image()))
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public DiscoveryProfile profile(String name) {
        String artist = requireValue(name, "artista");
        LastfmArtistInfoResponse response = lastfmClient.fetchArtistInfo(artist);
        if (response == null || response.artist() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Artista no encontrado: " + artist);
        }

        LastfmArtistInfoResponse.Artist info = response.artist();
        return new DiscoveryProfile(
                info.name(),
                artistImage(info.name()),
                cleanBio(info.bio()),
                info.stats() != null ? orZero(info.stats().listeners()) : "0",
                info.stats() != null ? orZero(info.stats().playcount()) : "0",
                names(info.tags() != null ? info.tags().tag() : null, LastfmArtistInfoResponse.Tag::name),
                names(info.similar() != null ? info.similar().artist() : null,
                        LastfmArtistInfoResponse.SimilarArtist::name)
        );
    }

    private static <T> List<String> names(List<T> source, java.util.function.Function<T, String> mapper) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(mapper).filter(java.util.Objects::nonNull).toList();
    }

    /** La bio trae HTML y un enlace de atribución al final que no aporta en la ficha. */
    private static String cleanBio(LastfmArtistInfoResponse.Bio bio) {
        if (bio == null || bio.summary() == null) {
            return "";
        }
        return bio.summary()
                .replaceAll("<a href.*?</a>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static long listenersOf(String listeners) {
        try {
            return listeners == null || listeners.isBlank() ? 0L : Long.parseLong(listeners.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Last.fm no distingue álbumes de singles, así que los tipos salen de MusicBrainz enlazando
     * por MBID. Las portadas siguen viniendo de Last.fm, que las tiene casi siempre.
     */
    private List<DiscoveryItem> discographyItems(String artist, int limit) {
        Map<String, String> covers = coversByTitle(artist);
        String mbid = artistMbid(artist);

        if (mbid != null && !mbid.isBlank()) {
            List<DiscoveryItem> releases = musicbrainzDiscographyService.findReleases(
                    artist, mbid, title -> covers.get(AlbumTitleFilter.normalize(title)), limit);
            if (!releases.isEmpty()) {
                return releases;
            }
        }

        return lastfmDiscography(artist, limit);
    }

    private String artistMbid(String artist) {
        try {
            LastfmArtistInfoResponse response = lastfmClient.fetchArtistInfo(artist);
            return response == null || response.artist() == null ? null : response.artist().mbid();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> coversByTitle(String artist) {
        Map<String, String> covers = new HashMap<>();
        try {
            LastfmTopAlbumsResponse response = lastfmClient.fetchArtistAlbums(artist, 100);
            if (response == null || response.topalbums() == null || response.topalbums().album() == null) {
                return covers;
            }
            for (LastfmTopAlbumsResponse.AlbumEntry album : response.topalbums().album()) {
                String image = LastfmImages.largest(album.image());
                if (image != null) {
                    covers.putIfAbsent(AlbumTitleFilter.normalize(album.name()), image);
                }
            }
        } catch (Exception ignored) {
            // Sin portadas se usa Cover Art Archive.
        }
        return covers;
    }

    /** Respaldo cuando el artista no tiene MBID o MusicBrainz no responde. */
    private List<DiscoveryItem> lastfmDiscography(String artist, int limit) {
        LastfmTopAlbumsResponse response = lastfmClient.fetchArtistAlbums(artist, 100);
        if (response == null || response.topalbums() == null || response.topalbums().album() == null) {
            return List.of();
        }

        Map<String, Candidate> unicos = new LinkedHashMap<>();
        Map<String, Long> playcounts = new HashMap<>();

        for (LastfmTopAlbumsResponse.AlbumEntry album : response.topalbums().album()) {
            String imageUrl = LastfmImages.largest(album.image());
            if (!AlbumTitleFilter.isAlbum(album, imageUrl)) {
                continue;
            }

            String key = AlbumTitleFilter.normalize(album.name());
            if (key.isEmpty()) {
                continue;
            }

            // Ante ediciones repetidas se conserva la más escuchada.
            long plays = AlbumTitleFilter.playcountOf(album);
            if (unicos.containsKey(key) && plays <= playcounts.get(key)) {
                continue;
            }

            String artistName = album.artist() != null ? album.artist().name() : artist;
            playcounts.put(key, plays);
            unicos.put(key, new Candidate(
                    new DiscoveryItem(
                            externalId(album.mbid(), artistName, album.name()),
                            AlbumTitleFilter.display(album.name()),
                            artistName,
                            "%s reproducciones".formatted(album.playcount()),
                            imageUrl,
                            artistName),
                    album.name()));
        }

        removeRedundantParents(unicos);

        List<Candidate> candidatos = unicos.values().stream().limit(DISCOGRAPHY_POOL).toList();
        return classifyReleases(artist, candidatos).stream().limit(limit).toList();
    }

    /** Título tal cual lo devuelve Last.fm, necesario para volver a consultarlo en album.getInfo. */
    private record Candidate(DiscoveryItem item, String rawName) {
    }

    /**
     * Last.fm publica singles y entradas sueltas como si fueran álbumes. Solo cuenta como disco
     * lo que album.getInfo confirma con un tracklist de varios temas.
     */
    private List<DiscoveryItem> classifyReleases(String artist, List<Candidate> candidatos) {
        return parallelMap(candidatos, candidate -> {
            DiscoveryItem item = candidate.item();
            int tracks = trackCountOf(artist, item.title(), candidate.rawName());
            return new DiscoveryItem(
                    item.externalId(),
                    item.title(),
                    item.subtitle(),
                    item.description(),
                    item.imageUrl(),
                    item.genre(),
                    tracks >= MIN_ALBUM_TRACKS ? "album" : "single");
        }, ALBUM_INFO_THREADS);
    }

    /** El título limpio es el que Last.fm indexa; el crudo suele traer sufijos que dan 404. */
    private int trackCountOf(String artist, String displayName, String rawName) {
        int tracks = trackCountOf(artist, displayName);
        if (tracks == 0 && !rawName.equals(displayName)) {
            return trackCountOf(artist, rawName);
        }
        return tracks;
    }

    private int trackCountOf(String artist, String rawAlbumName) {
        try {
            LastfmAlbumInfoResponse info = lastfmClient.fetchAlbumInfo(artist, rawAlbumName);
            return info == null ? 0 : info.trackCount();
        } catch (Exception e) {
            // Un 404 significa que Last.fm ni siquiera reconoce el título como álbum.
            return 0;
        }
    }

    /**
     * Si un disco está publicado en partes ("El Último Concierto A" y "B"), el título genérico
     * sin parte es una entrada repetida de lo mismo.
     */
    private void removeRedundantParents(Map<String, ?> albums) {
        List<String> keys = List.copyOf(albums.keySet());
        List<String> redundantes = keys.stream()
                .filter(key -> keys.stream()
                        .filter(other -> !other.equals(key) && other.startsWith(key + " "))
                        .count() >= 2)
                .toList();

        redundantes.forEach(albums::remove);
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
        return parallelMap(source, mapper, DEFAULT_THREADS);
    }

    private <T> List<DiscoveryItem> parallelMap(
            List<T> source, java.util.function.Function<T, DiscoveryItem> mapper, int threads) {
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
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
