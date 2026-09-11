package com.tapecloud.auth.review.service;

import com.tapecloud.auth.review.dto.CommentRequest;
import com.tapecloud.auth.review.dto.CommentResponse;
import com.tapecloud.auth.review.entity.Comment;
import com.tapecloud.auth.review.entity.Review;
import com.tapecloud.auth.review.repository.CommentRepository;
import com.tapecloud.auth.review.repository.ReviewRepository;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.repository.AppUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final AppUserRepository userRepository;

    public CommentService(
            CommentRepository commentRepository,
            ReviewRepository reviewRepository,
            AppUserRepository userRepository
    ) {
        this.commentRepository = commentRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findByReview(UUID reviewId) {
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(UUID reviewId, CommentRequest request, String userEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseña no encontrada"));

        AppUser user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail().split("@")[0];

        Comment comment = new Comment(
                review,
                user.getEmail(),
                displayName,
                request.body().trim()
        );

        return toResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(UUID commentId, String userEmail, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));

        if (!isAdmin && !comment.getAuthorEmail().equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para eliminar este comentario");
        }

        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getReview().getId(),
                comment.getAuthorEmail(),
                comment.getAuthorDisplayName(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}
