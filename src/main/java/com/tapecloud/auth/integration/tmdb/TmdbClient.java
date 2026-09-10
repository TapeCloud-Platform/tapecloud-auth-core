package com.tapecloud.auth.integration.tmdb;

import com.tapecloud.auth.integration.tmdb.dto.TmdbMoviePageResponse;
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

        String uri = UriComponentsBuilder.fromPath("/movie/popular")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("language", properties.getLanguage())
                .queryParam("page", page)
                .build()
                .toUriString();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(TmdbMoviePageResponse.class);
    }

    public String posterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return properties.getImageBaseUrl() + posterPath;
    }
}
