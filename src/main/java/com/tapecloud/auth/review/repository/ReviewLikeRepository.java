package com.tapecloud.auth.review.repository;

import com.tapecloud.auth.review.entity.ReviewLike;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, UUID> {

    long countByReviewId(UUID reviewId);

    boolean existsByReviewIdAndUserEmailIgnoreCase(UUID reviewId, String userEmail);

    Optional<ReviewLike> findByReviewIdAndUserEmailIgnoreCase(UUID reviewId, String userEmail);

    void deleteByReviewId(UUID reviewId);

    void deleteByUserEmailIgnoreCase(String userEmail);

    @Query("SELECT l.review.id, COUNT(l) FROM ReviewLike l WHERE l.review.id IN :ids GROUP BY l.review.id")
    List<Object[]> countGroupedByReviewIds(@Param("ids") List<UUID> ids);

    @Query("SELECT l.review.id FROM ReviewLike l WHERE l.review.id IN :ids AND LOWER(l.userEmail) = LOWER(:email)")
    Set<UUID> findLikedIdsByUser(@Param("ids") List<UUID> ids, @Param("email") String email);
}
