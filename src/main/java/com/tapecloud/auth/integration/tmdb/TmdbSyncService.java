package com.tapecloud.auth.integration.tmdb;

import com.tapecloud.auth.content.dto.ContentItemRequest;
import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.service.ContentItemService;
import com.tapecloud.auth.integration.tmdb.dto.TmdbMovieDto;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TmdbSyncService {

    private static final String SOURCE_APP = "tapeflix";
    private static final String SOURCE_TYPE = "movie";

    private final TmdbClient tmdbClient;
    private final ContentItemService contentItemService;

    public TmdbSyncService(TmdbClient tmdbClient, ContentItemService contentItemService) {
        this.tmdbClient = tmdbClient;
        this.contentItemService = contentItemService;
    }

    public List<ContentItem> syncPopularMovies(int page) {
        return tmdbClient.fetchPopularMovies(page).results().stream()
                .map(this::toContentItem)
                .map(contentItemService::save)
                .toList();
    }

    private ContentItemRequest toContentItem(TmdbMovieDto movie) {
        return new ContentItemRequest(
                SOURCE_APP,
                SOURCE_TYPE,
                String.valueOf(movie.id()),
                movie.title(),
                movie.overview(),
                tmdbClient.posterUrl(movie.posterPath()),
                parseReleaseDate(movie.releaseDate())
        );
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
