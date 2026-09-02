package com.tapecloud.sso.content.repository;

import com.tapecloud.sso.content.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByReviewIdOrderByCreatedAtAsc(UUID reviewId);
}
