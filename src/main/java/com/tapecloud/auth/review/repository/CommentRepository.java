package com.tapecloud.auth.review.repository;

import com.tapecloud.auth.review.entity.Comment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByReviewIdOrderByCreatedAtAsc(UUID reviewId);

    Page<Comment> findByReviewIdOrderByCreatedAtAsc(UUID reviewId, Pageable pageable);

    long countByReviewId(UUID reviewId);

    @Query("SELECT c.review.id, COUNT(c) FROM Comment c WHERE c.review.id IN :ids GROUP BY c.review.id")
    List<Object[]> countGroupedByReviewIds(@Param("ids") List<UUID> ids);

    void deleteByReviewId(UUID reviewId);

    void deleteByAuthorEmailIgnoreCase(String authorEmail);

    Optional<Comment> findTopByAuthorEmailIgnoreCaseOrderByCreatedAtDesc(String authorEmail);
}
