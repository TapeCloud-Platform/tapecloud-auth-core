package com.tapecloud.sso.content.repository;

import com.tapecloud.sso.content.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByContentIdOrderByCreatedAtDesc(UUID contentId);
    boolean existsByContentIdAndAuthorEmailIgnoreCase(UUID contentId, String authorEmail);
}
