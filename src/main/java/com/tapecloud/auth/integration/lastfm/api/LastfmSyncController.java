package com.tapecloud.auth.integration.lastfm.api;

import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.integration.lastfm.LastfmSyncService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/sync/lastfm")
public class LastfmSyncController {

    private final LastfmSyncService lastfmSyncService;

    public LastfmSyncController(LastfmSyncService lastfmSyncService) {
        this.lastfmSyncService = lastfmSyncService;
    }

    @PostMapping("/tracks")
    public List<ContentItem> syncTopTracks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return lastfmSyncService.syncTopTracks(page, limit);
    }
}
