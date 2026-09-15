package com.tapecloud.auth.integration.lastfm;

import com.tapecloud.auth.content.dto.ContentItemRequest;
import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.service.ContentItemService;
import com.tapecloud.auth.integration.lastfm.dto.LastfmImages;
import com.tapecloud.auth.integration.lastfm.dto.LastfmTrackDto;
import com.tapecloud.auth.integration.lastfm.dto.LastfmTrackInfoResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LastfmSyncService {

    private static final String SOURCE_APP = "tapebeat";
    private static final String SOURCE_TYPE = "track";

    private final LastfmClient lastfmClient;
    private final ContentItemService contentItemService;

    public LastfmSyncService(LastfmClient lastfmClient, ContentItemService contentItemService) {
        this.lastfmClient = lastfmClient;
        this.contentItemService = contentItemService;
    }

    public List<ContentItem> syncTopTracks(int page, int limit) {
        List<LastfmTrackDto> tracks = lastfmClient.fetchTopTracks(page, limit).tracks().track();

        return tracks.stream()
                .map(this::toContentItem)
                .map(contentItemService::save)
                .toList();
    }

    private ContentItemRequest toContentItem(LastfmTrackDto track) {
        String artistName = track.artist() != null ? track.artist().name() : "Artista desconocido";
        String externalId = track.mbid() != null && !track.mbid().isBlank()
                ? track.mbid()
                : artistName + "-" + track.name();

        return new ContentItemRequest(
                SOURCE_APP,
                SOURCE_TYPE,
                externalId,
                track.name(),
                buildDescription(track, artistName),
                resolveImageUrl(track, artistName),
                null,
                artistName
        );
    }

    private String resolveImageUrl(LastfmTrackDto track, String artistName) {
        String chartImage = track.imageUrl();
        if (chartImage != null) {
            return chartImage;
        }

        try {
            LastfmTrackInfoResponse info = lastfmClient.fetchTrackInfo(artistName, track.name());
            if (info != null && info.track() != null && info.track().album() != null) {
                return LastfmImages.largest(info.track().album().image());
            }
        } catch (Exception ignored) {
            // Si track.getInfo falla, el item se guarda sin portada.
        }
        return null;
    }

    private String buildDescription(LastfmTrackDto track, String artistName) {
        String listeners = track.listeners() != null ? track.listeners() : "0";
        String playcount = track.playcount() != null ? track.playcount() : "0";
        return "%s · %s oyentes · %s reproducciones".formatted(artistName, listeners, playcount);
    }
}
