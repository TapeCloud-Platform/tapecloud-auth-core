package com.tapecloud.auth.integration.lastfm;

import com.tapecloud.auth.integration.lastfm.dto.LastfmChartResponse;
import com.tapecloud.auth.integration.lastfm.dto.LastfmSearchResponse;
import com.tapecloud.auth.integration.lastfm.dto.LastfmTrackInfoResponse;
import java.util.function.UnaryOperator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@EnableConfigurationProperties(LastfmProperties.class)
public class LastfmClient {

    private final RestClient restClient;
    private final LastfmProperties properties;

    public LastfmClient(LastfmProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    public LastfmChartResponse fetchTopTracks(int page, int limit) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("LASTFM_API_KEY no está configurada");
        }

        String uri = UriComponentsBuilder.fromPath("/")
                .queryParam("method", "chart.gettoptracks")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("format", "json")
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build()
                .toUriString();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(LastfmChartResponse.class);
    }

    // chart.gettoptracks solo devuelve la imagen placeholder; la portada real viene de track.getInfo.
    public LastfmTrackInfoResponse fetchTrackInfo(String artist, String track) {
        String uri = UriComponentsBuilder.fromPath("/")
                .queryParam("method", "track.getInfo")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("format", "json")
                .queryParam("artist", artist)
                .queryParam("track", track)
                .encode()
                .build()
                .toUriString();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(LastfmTrackInfoResponse.class);
    }

    public LastfmChartResponse fetchTracksByTag(String tag, int limit) {
        return getChart(builder -> builder
                .queryParam("method", "tag.getTopTracks")
                .queryParam("tag", tag)
                .queryParam("limit", limit));
    }

    public LastfmChartResponse fetchTracksByArtist(String artist, int limit) {
        return getChart(builder -> builder
                .queryParam("method", "artist.getTopTracks")
                .queryParam("artist", artist)
                .queryParam("limit", limit));
    }

    public LastfmChartResponse fetchTracksByCountry(String country, int limit) {
        return getChart(builder -> builder
                .queryParam("method", "geo.getTopTracks")
                .queryParam("country", country)
                .queryParam("limit", limit));
    }

    public LastfmSearchResponse searchTracks(String query, int limit) {
        String uri = baseBuilder()
                .queryParam("method", "track.search")
                .queryParam("track", query)
                .queryParam("limit", limit)
                .encode()
                .build()
                .toUriString();

        return restClient.get().uri(uri).retrieve().body(LastfmSearchResponse.class);
    }

    private LastfmChartResponse getChart(UnaryOperator<UriComponentsBuilder> customizer) {
        requireApiKey();
        String uri = customizer.apply(baseBuilder()).encode().build().toUriString();
        return restClient.get().uri(uri).retrieve().body(LastfmChartResponse.class);
    }

    private UriComponentsBuilder baseBuilder() {
        return UriComponentsBuilder.fromPath("/")
                .queryParam("api_key", properties.getApiKey())
                .queryParam("format", "json");
    }

    private void requireApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("LASTFM_API_KEY no está configurada");
        }
    }
}
