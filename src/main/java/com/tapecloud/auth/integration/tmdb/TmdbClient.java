package com.tapecloud.auth.integration.tmdb;

import com.tapecloud.auth.integration.tmdb.dto.TmdbGenreListResponse;
import com.tapecloud.auth.integration.tmdb.dto.TmdbMoviePageResponse;
import com.tapecloud.auth.integration.tmdb.dto.TmdbPersonSearchResponse;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbClient {

    private final RestClient restClient;
    private final TmdbProperties properties;

    public TmdbClient(TmdbProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    public TmdbMoviePageResponse fetchPopularMovies(int page) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("TMDB_API_KEY no está configurada");
        }

        String uri = UriComponentsBuilder.fromPath("/discover/movie")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("language", properties.getLanguage())
                .queryParam("with_genres", "16")
                .queryParam("sort_by", "popularity.desc")
                .queryParam("page", page)
                .build()
                .toUriString();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(TmdbMoviePageResponse.class);
    }

    public TmdbGenreListResponse fetchMovieGenres() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return new TmdbGenreListResponse(List.of());
        }

        String uri = UriComponentsBuilder.fromPath("/genre/movie/list")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("language", properties.getLanguage())
                .build()
                .toUriString();

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(TmdbGenreListResponse.class);
        } catch (Exception e) {
            return new TmdbGenreListResponse(List.of());
        }
    }

    public TmdbMoviePageResponse discoverMovies(String extraParam, String extraValue, int page) {
        requireApiKey();

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/discover/movie")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("language", properties.getLanguage())
                .queryParam("sort_by", "popularity.desc")
                .queryParam("page", page);

        if (extraParam != null && extraValue != null) {
            builder.queryParam(extraParam, extraValue);
        }

        return restClient.get()
                .uri(builder.encode().build().toUriString())
                .retrieve()
                .body(TmdbMoviePageResponse.class);
    }

    public TmdbMoviePageResponse searchMovies(String query, int page) {
        requireApiKey();

        String uri = UriComponentsBuilder.fromPath("/search/movie")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("language", properties.getLanguage())
                .queryParam("query", query)
                .queryParam("page", page)
                .encode()
                .build()
                .toUriString();

        return restClient.get().uri(uri).retrieve().body(TmdbMoviePageResponse.class);
    }

    public TmdbPersonSearchResponse searchPerson(String query) {
        requireApiKey();

        String uri = UriComponentsBuilder.fromPath("/search/person")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("language", properties.getLanguage())
                .queryParam("query", query)
                .encode()
                .build()
                .toUriString();

        return restClient.get().uri(uri).retrieve().body(TmdbPersonSearchResponse.class);
    }

    private void requireApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("TMDB_API_KEY no está configurada");
        }
    }

    public String posterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return properties.getImageBaseUrl() + posterPath;
    }
}

