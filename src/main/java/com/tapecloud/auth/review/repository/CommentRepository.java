package com.tapecloud.auth.review.repository;

import com.tapecloud.auth.review.entity.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByReviewIdOrderByCreatedAtAsc(UUID reviewId);

    long countByReviewId(UUID reviewId);
}
