package com.tapecloud.auth.integration.musicbrainz;

import com.tapecloud.auth.integration.musicbrainz.dto.MusicbrainzArtistSearchResponse;
import com.tapecloud.auth.integration.musicbrainz.dto.MusicbrainzReleaseGroupResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@EnableConfigurationProperties(MusicbrainzProperties.class)
public class MusicbrainzClient {

    private final RestClient restClient;
    private final MusicbrainzRateLimiter rateLimiter = new MusicbrainzRateLimiter();

    public MusicbrainzClient(MusicbrainzProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
    }

    @Cacheable(value = "mbArtists", unless = "#result == null")
    public MusicbrainzArtistSearchResponse searchArtist(String name) {
        String uri = UriComponentsBuilder.fromPath("/artist")
                .queryParam("query", "artist:\"%s\"".formatted(name))
                .queryParam("fmt", "json")
                .queryParam("limit", 1)
                .encode()
                .build()
                .toUriString();

        return rateLimiter.call(() ->
                restClient.get().uri(uri).retrieve().body(MusicbrainzArtistSearchResponse.class));
    }

    /** Un release-group agrupa todas las ediciones de un álbum, así que no se repiten remasters. */
    @Cacheable(value = "mbAlbums", unless = "#result == null")
    public MusicbrainzReleaseGroupResponse fetchReleaseGroups(String artistMbid, int limit) {
        String uri = UriComponentsBuilder.fromPath("/release-group")
                .queryParam("artist", artistMbid)
                .queryParam("fmt", "json")
                .queryParam("limit", limit)
                .encode()
                .build()
                .toUriString();

        return rateLimiter.call(() ->
                restClient.get().uri(uri).retrieve().body(MusicbrainzReleaseGroupResponse.class));
    }
}
