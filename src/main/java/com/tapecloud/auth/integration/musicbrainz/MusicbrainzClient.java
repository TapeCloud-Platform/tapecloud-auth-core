package com.tapecloud.auth.integration.musicbrainz;

import com.tapecloud.auth.integration.musicbrainz.dto.MusicbrainzArtistSearchResponse;
import com.tapecloud.auth.integration.musicbrainz.dto.MusicbrainzReleaseGroupResponse;
import java.net.URI;
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
    private final MusicbrainzProperties properties;

    public MusicbrainzClient(MusicbrainzProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
    }

    @Cacheable(value = "mbArtists", unless = "#result == null")
    public MusicbrainzArtistSearchResponse searchArtist(String name) {
        URI uri = baseBuilder("/artist")
                .queryParam("query", "artist:\"%s\"".formatted(name))
                .queryParam("limit", 1)
                .encode()
                .build()
                .toUri();

        return rateLimiter.call(() ->
                restClient.get().uri(uri).retrieve().body(MusicbrainzArtistSearchResponse.class));
    }

    /** Un release-group agrupa todas las ediciones de un álbum, así que no se repiten remasters. */
    @Cacheable(value = "mbAlbums", unless = "#result == null")
    public MusicbrainzReleaseGroupResponse fetchReleaseGroups(String artistMbid, int limit) {
        URI uri = baseBuilder("/release-group")
                .queryParam("artist", artistMbid)
                .queryParam("limit", limit)
                .encode()
                .build()
                .toUri();

        return rateLimiter.call(() ->
                restClient.get().uri(uri).retrieve().body(MusicbrainzReleaseGroupResponse.class));
    }

    // URI absoluta armada a mano (con esquema y host): RestClient.uri(String) re-codifica un string
    // ya codificado, lo que rompía nombres con tildes o ñ. Con RestClient.uri(URI) no vuelve a tocarla.
    private UriComponentsBuilder baseBuilder(String path) {
        return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(path)
                .queryParam("fmt", "json");
    }
}
