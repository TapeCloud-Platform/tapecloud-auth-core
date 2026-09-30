package com.tapecloud.auth.integration.tmdb;

import com.tapecloud.auth.discovery.DiscoveryFilter;
import com.tapecloud.auth.discovery.DiscoveryFilterOption;
import com.tapecloud.auth.discovery.DiscoveryItem;
import com.tapecloud.auth.discovery.DiscoveryProvider;
import com.tapecloud.auth.integration.tmdb.dto.TmdbGenreDto;
import com.tapecloud.auth.integration.tmdb.dto.TmdbMovieDto;
import com.tapecloud.auth.integration.tmdb.dto.TmdbMoviePageResponse;
import com.tapecloud.auth.integration.tmdb.dto.TmdbPersonSearchResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TmdbDiscoveryProvider implements DiscoveryProvider {

    private static final List<DiscoveryFilterOption> COUNTRIES = List.of(
            new DiscoveryFilterOption("AR", "Argentina"),
            new DiscoveryFilterOption("BR", "Brasil"),
            new DiscoveryFilterOption("ES", "España"),
            new DiscoveryFilterOption("MX", "México"),
            new DiscoveryFilterOption("US", "Estados Unidos"),
            new DiscoveryFilterOption("GB", "Reino Unido"),
            new DiscoveryFilterOption("JP", "Japón"),
            new DiscoveryFilterOption("KR", "Corea del Sur"),
            new DiscoveryFilterOption("FR", "Francia")
    );

    private final TmdbClient tmdbClient;

    public TmdbDiscoveryProvider(TmdbClient tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    @Override
    public String sourceApp() {
        return "tapeflix";
    }

    @Override
    public List<DiscoveryFilter> availableFilters() {
        return List.of(
                new DiscoveryFilter("top", "Más populares", false, List.of()),
                new DiscoveryFilter("genre", "Género", false, genreOptions()),
                new DiscoveryFilter("country", "País", false, COUNTRIES),
                new DiscoveryFilter("artist", "Actor / Director", true, List.of()),
                new DiscoveryFilter("search", "Buscar película", true, List.of())
        );
    }

    @Override
    public List<DiscoveryItem> discover(String type, String value, int limit) {
        TmdbMoviePageResponse response = switch (type) {
            case "top" -> tmdbClient.discoverMovies(null, null, 1);
            case "genre" -> tmdbClient.discoverMovies("with_genres", requireValue(value, "género"), 1);
            case "country" -> tmdbClient.discoverMovies("with_origin_country", requireValue(value, "país"), 1);
            case "artist" -> byPerson(requireValue(value, "actor o director"));
            case "search" -> tmdbClient.searchMovies(requireValue(value, "búsqueda"), 1);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro no soportado: " + type);
        };

        if (response == null || response.results() == null) {
            return List.of();
        }

        Map<Integer, String> genres = genreMap();
        return response.results().stream()
                .limit(limit)
                .map(movie -> toItem(movie, genres))
                .toList();
    }

    private TmdbMoviePageResponse byPerson(String name) {
        TmdbPersonSearchResponse people = tmdbClient.searchPerson(name);
        if (people == null || people.results() == null || people.results().isEmpty()) {
            return null;
        }
        Long personId = people.results().get(0).id();
        return tmdbClient.discoverMovies("with_people", String.valueOf(personId), 1);
    }

    private DiscoveryItem toItem(TmdbMovieDto movie, Map<Integer, String> genres) {
        String genre = resolveGenre(movie, genres);
        return new DiscoveryItem(
                String.valueOf(movie.id()),
                movie.title(),
                movie.releaseDate(),
                movie.overview(),
                tmdbClient.posterUrl(movie.posterPath()),
                genre
        );
    }

    private String resolveGenre(TmdbMovieDto movie, Map<Integer, String> genres) {
        if (movie.genreIds() == null || movie.genreIds().isEmpty()) {
            return "General";
        }
        String joined = movie.genreIds().stream()
                .map(genres::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return joined.isBlank() ? "General" : joined;
    }

    private Map<Integer, String> genreMap() {
        List<TmdbGenreDto> genres = tmdbClient.fetchMovieGenres().genres();
        if (genres == null) {
            return Map.of();
        }
        return genres.stream().collect(Collectors.toMap(TmdbGenreDto::id, TmdbGenreDto::name, (a, b) -> a));
    }

    private List<DiscoveryFilterOption> genreOptions() {
        return genreMap().entrySet().stream()
                .map(entry -> new DiscoveryFilterOption(String.valueOf(entry.getKey()), entry.getValue()))
                .sorted((a, b) -> a.label().compareToIgnoreCase(b.label()))
                .toList();
    }

    private String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el valor de " + label);
        }
        return value;
    }
}
