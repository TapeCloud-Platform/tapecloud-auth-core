package com.tapecloud.auth.review.repository;

import com.tapecloud.auth.review.entity.ReviewLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, UUID> {

    long countByReviewId(UUID reviewId);

    boolean existsByReviewIdAndUserEmailIgnoreCase(UUID reviewId, String userEmail);

    Optional<ReviewLike> findByReviewIdAndUserEmailIgnoreCase(UUID reviewId, String userEmail);
}
