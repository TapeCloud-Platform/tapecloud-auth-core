package com.tapecloud.auth.review.repository;

import com.tapecloud.auth.review.entity.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByContentIdOrderByCreatedAtDesc(UUID contentId);

    List<Review> findByContentSourceAppOrderByCreatedAtDesc(String sourceApp);

    List<Review> findByAuthorEmailOrderByCreatedAtDesc(String authorEmail);

    boolean existsByContentIdAndAuthorEmailIgnoreCase(UUID contentId, String authorEmail);

    long countByContentSourceApp(String sourceApp);

    long countByAuthorEmail(String authorEmail);

    long countByAuthorEmailAndContentSourceApp(String authorEmail, String sourceApp);
}
