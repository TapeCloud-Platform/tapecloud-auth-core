package com.tapecloud.sso.content.repository;

import com.tapecloud.sso.content.entity.ContentApp;
import com.tapecloud.sso.content.entity.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaItemRepository extends JpaRepository<MediaItem, UUID> {
    List<MediaItem> findByAppOrderByTitleAsc(ContentApp app);
    List<MediaItem> findByAppAndTitleContainingIgnoreCase(ContentApp app, String title);
}
