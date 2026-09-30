package com.tapecloud.auth.integration.tmdb;

import com.tapecloud.auth.content.dto.ContentItemRequest;
import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.service.ContentItemService;
import com.tapecloud.auth.integration.tmdb.dto.TmdbGenreDto;
import com.tapecloud.auth.integration.tmdb.dto.TmdbMovieDto;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TmdbSyncService {

    private static final String SOURCE_APP = "tapeflix";
    private static final String SOURCE_TYPE = "movie";

    private static final Map<Integer, String> FALLBACK_GENRES = Map.ofEntries(
            Map.entry(28, "Acción"),
            Map.entry(12, "Aventura"),
            Map.entry(16, "Animación"),
            Map.entry(35, "Comedia"),
            Map.entry(80, "Crimen"),
            Map.entry(99, "Documental"),
            Map.entry(18, "Drama"),
            Map.entry(10751, "Familia"),
            Map.entry(14, "Fantasía"),
            Map.entry(36, "Historia"),
            Map.entry(27, "Terror"),
            Map.entry(10402, "Música"),
            Map.entry(9648, "Misterio"),
            Map.entry(10749, "Romance"),
            Map.entry(878, "Ciencia ficción"),
            Map.entry(10770, "Película de TV"),
            Map.entry(53, "Suspense"),
            Map.entry(10752, "Bélica"),
            Map.entry(37, "Western")
    );

    private final TmdbClient tmdbClient;
    private final ContentItemService contentItemService;

    public TmdbSyncService(TmdbClient tmdbClient, ContentItemService contentItemService) {
        this.tmdbClient = tmdbClient;
        this.contentItemService = contentItemService;
    }

    public List<ContentItem> syncPopularMovies(int page) {
        Map<Integer, String> genreMap = fetchGenreMap();

        return tmdbClient.fetchPopularMovies(page).results().stream()
                .map(movie -> toContentItem(movie, genreMap))
                .map(contentItemService::save)
                .toList();
    }

    private Map<Integer, String> fetchGenreMap() {
        try {
            List<TmdbGenreDto> genres = tmdbClient.fetchMovieGenres().genres();
            if (genres != null && !genres.isEmpty()) {
                return genres.stream().collect(Collectors.toMap(TmdbGenreDto::id, TmdbGenreDto::name, (a, b) -> a));
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_GENRES;
    }

    private ContentItemRequest toContentItem(TmdbMovieDto movie, Map<Integer, String> genreMap) {
        return new ContentItemRequest(
                SOURCE_APP,
                SOURCE_TYPE,
                String.valueOf(movie.id()),
                movie.title(),
                movie.overview(),
                tmdbClient.posterUrl(movie.posterPath()),
                parseReleaseDate(movie.releaseDate()),
                resolveGenre(movie.genreIds(), genreMap)
        );
    }

    private String resolveGenre(List<Integer> genreIds, Map<Integer, String> genreMap) {
        if (genreIds == null || genreIds.isEmpty()) {
            return "General";
        }
        String genres = genreIds.stream()
                .map(id -> genreMap.getOrDefault(id, FALLBACK_GENRES.get(id)))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return genres.isBlank() ? "General" : genres;
    }

    private LocalDate parseReleaseDate(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(releaseDate);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}

