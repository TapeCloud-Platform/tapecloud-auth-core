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
                new DiscoveryFilter("people", "Personas", true, List.of()),
                new DiscoveryFilter("search", "Buscar película", true, List.of())
        );
    }

    @Override
    public List<DiscoveryItem> discover(String type, String value, int limit) {
        if ("people".equals(type)) {
            return people(requireValue(value, "persona"), limit);
        }
        TmdbMoviePageResponse response = switch (type) {            case "top" -> tmdbClient.discoverMovies(null, null, 1);
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

    /**
     * Ficha de una persona para /person/:nombre. Resuelve el nombre al primer
     * match de TMDb y devuelve bio, foto, departamento y películas destacadas.
     */
    @Override
    public com.tapecloud.auth.discovery.DiscoveryProfile profile(String name) {
        TmdbPersonSearchResponse search = tmdbClient.searchPerson(requireValue(name, "persona"));
        if (search == null || search.results() == null || search.results().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + name);
        }
        var match = search.results().get(0);

        com.tapecloud.auth.integration.tmdb.dto.TmdbPersonDetailResponse detail;
        try {
            detail = tmdbClient.fetchPersonDetails(match.id());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada: " + name);
        }

        List<String> knownFor = List.of();
        try {
            var credits = tmdbClient.fetchPersonMovieCredits(match.id());
            if (credits != null && credits.cast() != null) {
                knownFor = credits.cast().stream()
                        .filter(c -> c.title() != null && !c.title().isBlank())
                        .sorted((a, b) -> Double.compare(
                                b.popularity() != null ? b.popularity() : 0,
                                a.popularity() != null ? a.popularity() : 0))
                        .limit(5)
                        .map(com.tapecloud.auth.integration.tmdb.dto.TmdbPersonCreditsResponse.TmdbCredit::title)
                        .toList();
            }
        } catch (Exception ignored) {
            // Sin créditos se devuelve igual la ficha con bio y foto.
        }

        String department = detail.knownForDepartment() != null
                ? detail.knownForDepartment()
                : match.knownForDepartment();
        List<String> tags = department != null ? List.of(department) : List.of();
        String bio = detail.biography() != null && !detail.biography().isBlank() ? detail.biography() : "";
        String listeners = detail.birthday() != null && !detail.birthday().isBlank()
                ? "Nació el " + detail.birthday()
                : "";
        String playcount = detail.popularity() != null ? String.valueOf(detail.popularity().intValue()) : "0";

        return new com.tapecloud.auth.discovery.DiscoveryProfile(
                detail.name() != null ? detail.name() : match.name(),
                tmdbClient.posterUrl(detail.profilePath() != null ? detail.profilePath() : match.profilePath()),
                bio.isEmpty() && !knownFor.isEmpty()
                        ? "Conocido por: " + String.join(", ", knownFor)
                        : bio,
                listeners,
                playcount,
                tags,
                List.of(),
                "https://www.themoviedb.org/person/" + match.id());
    }

    /** Búsqueda directa de personas (actores/directores) para el header: devuelve perfiles, no películas. */
    private List<DiscoveryItem> people(String query, int limit) {
        TmdbPersonSearchResponse response = tmdbClient.searchPerson(query);
        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream()
                .filter(p -> p.name() != null && !p.name().isBlank())
                .limit(limit)
                .map(p -> new DiscoveryItem(
                        "person:" + p.id(),
                        p.name(),
                        p.knownForDepartment() != null ? p.knownForDepartment() : "Persona",
                        p.knownForDepartment() != null ? p.knownForDepartment() : "Persona",
                        tmdbClient.posterUrl(p.profilePath()),
                        p.name(),
                        "person"))
                .toList();
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

    /**
     * Filtros combinados ("grunge francés"): género + país + persona en un
     * solo discover/movie; la búsqueda libre filtra por género/país en memoria.
     */
    @Override
    public List<DiscoveryItem> discoverCombined(Map<String, String> filters, int limit) {
        String genre = blankToNull(filters.get("genre"));
        String country = blankToNull(filters.get("country"));
        String artist = blankToNull(firstPresent(filters, "artist", "people"));
        String search = blankToNull(filters.get("search"));

        if (search != null && genre == null && country == null && artist == null) {
            return discover("search", search, limit);
        }

        Map<Integer, String> genres = genreMap();
        if (search != null) {
            TmdbMoviePageResponse response = tmdbClient.searchMovies(search, 1);
            if (response == null || response.results() == null) {
                return List.of();
            }
            java.util.Set<Integer> genreIds = parseGenreIds(genre);
            return response.results().stream()
                    .filter(m -> genreIds.isEmpty() || matchesAnyGenre(m, genreIds))
                    .filter(m -> country == null || matchesCountry(m, country))
                    .limit(limit)
                    .map(movie -> toItem(movie, genres))
                    .toList();
        }

        Map<String, String> params = new java.util.LinkedHashMap<>();
        if (genre != null) {
            params.put("with_genres", genre);
        }
        if (country != null) {
            params.put("with_origin_country", country);
        }
        if (artist != null) {
            String personIds = personIdsOf(artist);
            if (personIds == null) {
                return List.of();
            }
            params.put("with_people", personIds);
        }
        if (params.isEmpty()) {
            return discover("top", null, limit);
        }
        TmdbMoviePageResponse response = tmdbClient.discoverMovies(params, 1);
        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream()
                .limit(limit)
                .map(movie -> toItem(movie, genres))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String firstPresent(Map<String, String> filters, String... keys) {
        for (String key : keys) {
            String value = blankToNull(filters.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static java.util.Set<Integer> parseGenreIds(String genre) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        if (genre == null) {
            return ids;
        }
        for (String part : genre.split("[,|]")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // Un nombre en vez de id simplemente no filtra.
            }
        }
        return ids;
    }

    private static boolean matchesAnyGenre(TmdbMovieDto movie, java.util.Set<Integer> genreIds) {
        if (movie.genreIds() == null) {
            return false;
        }
        return movie.genreIds().stream().anyMatch(genreIds::contains);
    }

    private static boolean matchesCountry(TmdbMovieDto movie, String country) {
        if (movie.originCountries() == null) {
            return false;
        }
        return movie.originCountries().stream().anyMatch(c -> c.equalsIgnoreCase(country));
    }

    private Long personIdOf(String name) {
        try {
            TmdbPersonSearchResponse people = tmdbClient.searchPerson(name);
            if (people == null || people.results() == null || people.results().isEmpty()) {
                return null;
            }
            return people.results().get(0).id();
        } catch (Exception e) {
            return null;
        }
    }

    /** Varios artistas ("brad pitt,tom hanks"): une sus ids con coma (AND de TMDb). */
    private String personIdsOf(String names) {
        List<String> ids = new java.util.ArrayList<>();
        for (String part : names.split("[,|]")) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            Long id = personIdOf(name);
            if (id != null && !ids.contains(String.valueOf(id))) {
                ids.add(String.valueOf(id));
            }
        }
        return ids.isEmpty() ? null : String.join(",", ids);
    }
}
