package com.tapecloud.auth.content.repository;

import com.tapecloud.auth.content.entity.ContentItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    List<ContentItem> findAllBySourceAppOrderByUpdatedAtDesc(String sourceApp);

    List<ContentItem> findAllBySourceAppAndSourceTypeOrderByUpdatedAtDesc(String sourceApp, String sourceType);

    Optional<ContentItem> findBySourceAppAndSourceTypeAndExternalId(
            String sourceApp, String sourceType, String externalId);

    // Paginación por app
    Page<ContentItem> findBySourceAppOrderByUpdatedAtDesc(String sourceApp, Pageable pageable);

    // Paginación por app + tipo
    Page<ContentItem> findBySourceAppAndSourceTypeOrderByUpdatedAtDesc(String sourceApp, String sourceType, Pageable pageable);

    // Paginación por app + género
    Page<ContentItem> findBySourceAppAndGenreContainingIgnoreCaseOrderByUpdatedAtDesc(String sourceApp, String genre, Pageable pageable);

    // Paginación por app + tipo + género
    Page<ContentItem> findBySourceAppAndSourceTypeAndGenreContainingIgnoreCaseOrderByUpdatedAtDesc(String sourceApp, String sourceType, String genre, Pageable pageable);
}