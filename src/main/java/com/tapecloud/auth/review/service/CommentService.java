package com.tapecloud.auth.review.service;

import com.tapecloud.auth.moderation.ProfanityFilterService;
import com.tapecloud.auth.exception.TooManyAttemptsException;
import com.tapecloud.auth.review.dto.CommentRequest;
import com.tapecloud.auth.review.dto.CommentResponse;
import com.tapecloud.auth.review.entity.Comment;
import com.tapecloud.auth.review.entity.Review;
import com.tapecloud.auth.review.repository.CommentRepository;
import com.tapecloud.auth.review.repository.ReviewRepository;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.repository.AppUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final AppUserRepository userRepository;
    private final ProfanityFilterService profanityFilterService;

    @Value("${app.comment.cooldown-seconds:30}")
    private long commentCooldownSeconds;

    public CommentService(
            CommentRepository commentRepository,
            ReviewRepository reviewRepository,
            AppUserRepository userRepository,
            ProfanityFilterService profanityFilterService
    ) {
        this.commentRepository = commentRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.profanityFilterService = profanityFilterService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findByReview(UUID reviewId, String currentUserEmail) {
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId).stream()
                .map(comment -> toResponse(comment, currentUserEmail))
                .toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CommentResponse> findByReviewPaged(
            UUID reviewId, String currentUserEmail, org.springframework.data.domain.Pageable pageable) {
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId, pageable)
                .map(comment -> toResponse(comment, currentUserEmail));
    }

    @Transactional
    public CommentResponse createComment(UUID reviewId, CommentRequest request, String userEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseña no encontrada"));

        AppUser user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Un comentario cada 30 segundos por usuario (429 si es muy pronto),
        // igual que el cooldown de edición de reseñas.
        Instant now = Instant.now();
        commentRepository.findTopByAuthorEmailIgnoreCaseOrderByCreatedAtDesc(userEmail)
                .ifPresent(last -> {
                    // elapsed negativo (reloj desfasado) se trata como 0: espera el cooldown completo, nunca más.
                    long elapsed = Math.max(0, Duration.between(last.getCreatedAt(), now).getSeconds());
                    if (elapsed < commentCooldownSeconds) {
                        throw new TooManyAttemptsException(
                                "Podés volver a comentar en " + (commentCooldownSeconds - elapsed) + " segundos");
                    }
                });

        profanityFilterService.requireClean(request.body(), "El comentario");

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail().split("@")[0];

        Comment comment = new Comment(
                review,
                user.getEmail(),
                displayName,
                request.body().trim()
        );

        return toResponse(commentRepository.save(comment), userEmail);
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

    private CommentResponse toResponse(Comment comment, String currentUserEmail) {
        boolean ownedByCurrentUser = currentUserEmail != null &&
                !currentUserEmail.isBlank() &&
                comment.getAuthorEmail().equalsIgnoreCase(currentUserEmail);

        return new CommentResponse(
                comment.getId(),
                comment.getReview().getId(),
                comment.getAuthorDisplayName(),
                ownedByCurrentUser,
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}
