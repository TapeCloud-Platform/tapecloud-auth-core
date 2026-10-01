package com.tapecloud.auth.review.repository;

import com.tapecloud.auth.review.entity.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByContentIdOrderByCreatedAtDesc(UUID contentId);

    Page<Review> findByContentIdOrderByCreatedAtDesc(UUID contentId, Pageable pageable);

    List<Review> findByContentSourceAppOrderByCreatedAtDesc(String sourceApp);

    Page<Review> findByContentSourceAppOrderByCreatedAtDesc(String sourceApp, Pageable pageable);

    List<Review> findByAuthorEmailOrderByCreatedAtDesc(String authorEmail);

    List<Review> findByAuthorEmailIgnoreCase(String authorEmail);

    boolean existsByContentIdAndAuthorEmailIgnoreCase(UUID contentId, String authorEmail);

    long countByAuthorEmailAndContentSourceApp(String authorEmail, String sourceApp);

    @Query("""
            SELECT r FROM Review r LEFT JOIN ReviewLike l ON l.review = r
            WHERE LOWER(r.authorEmail) = LOWER(:email)
            GROUP BY r ORDER BY COUNT(l) DESC, r.createdAt DESC
            """)
    List<Review> findTopByAuthorEmailOrderByLikesDesc(@Param("email") String email, Pageable pageable);
}
