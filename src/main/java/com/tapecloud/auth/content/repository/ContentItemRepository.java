package com.tapecloud.auth.content.repository;

import com.tapecloud.auth.content.entity.ContentItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    List<ContentItem> findAllBySourceAppOrderByUpdatedAtDesc(String sourceApp);

    List<ContentItem> findAllBySourceAppAndSourceTypeOrderByUpdatedAtDesc(String sourceApp, String sourceType);

    Optional<ContentItem> findBySourceAppAndSourceTypeAndExternalId(
            String sourceApp, String sourceType, String externalId);
}