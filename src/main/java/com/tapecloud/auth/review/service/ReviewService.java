package com.tapecloud.auth.review.service;

import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.repository.ContentItemRepository;
import com.tapecloud.auth.moderation.ProfanityFilterService;
import com.tapecloud.auth.review.dto.ReviewRequest;
import com.tapecloud.auth.review.dto.ReviewResponse;
import com.tapecloud.auth.review.dto.ReviewStatsResponse;
import com.tapecloud.auth.exception.TooManyAttemptsException;
import com.tapecloud.auth.review.entity.Review;
import com.tapecloud.auth.review.entity.ReviewLike;
import com.tapecloud.auth.review.repository.CommentRepository;
import com.tapecloud.auth.review.repository.ReviewLikeRepository;
import com.tapecloud.auth.review.repository.ReviewRepository;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.repository.AppUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final CommentRepository commentRepository;
    private final ContentItemRepository contentItemRepository;
    private final AppUserRepository userRepository;
    private final ProfanityFilterService profanityFilterService;

    @Value("${app.review.edit-cooldown-seconds:30}")
    private long editCooldownSeconds;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewLikeRepository reviewLikeRepository,
            CommentRepository commentRepository,
            ContentItemRepository contentItemRepository,
            AppUserRepository userRepository,
            ProfanityFilterService profanityFilterService
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.commentRepository = commentRepository;
        this.contentItemRepository = contentItemRepository;
        this.userRepository = userRepository;
        this.profanityFilterService = profanityFilterService;
    }

    private void validateRating(Double rating) {
        if (rating == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La puntuación es obligatoria");
        }
        if (rating < 0.5 || rating > 5.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La puntuación debe estar entre 0.5 y 5");
        }
        double doubled = rating * 2.0;
        if (Math.abs(doubled - Math.round(doubled)) > 1e-9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La puntuación debe ser en incrementos de 0.5");
        }
    }

    @Transactional
    public ReviewResponse createReview(UUID contentId, ReviewRequest request, String userEmail) {
        ContentItem content = contentItemRepository.findById(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contenido no encontrado"));

        AppUser user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (reviewRepository.existsByContentIdAndAuthorEmailIgnoreCase(contentId, user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya publicaste una reseña para este contenido");
        }

        // Bloquear (con aviso) si hay lenguaje no permitido en título o cuerpo.
        profanityFilterService.requireClean(request.title(), "El título");
        profanityFilterService.requireClean(request.body(), "La reseña");

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail().split("@")[0];

        validateRating(request.rating());

        Review review = new Review(
                content,
                user.getEmail(),
                displayName,
                request.title().trim(),
                request.body().trim(),
                request.rating(),
                request.isSpoiler() != null ? request.isSpoiler() : Boolean.FALSE
        );

        return toResponse(reviewRepository.save(review), userEmail);
    }

    @Transactional
    public ReviewResponse toggleLike(UUID reviewId, String userEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseña no encontrada"));

        Optional<ReviewLike> existingLike = reviewLikeRepository.findByReviewIdAndUserEmailIgnoreCase(reviewId, userEmail);
        if (existingLike.isPresent()) {
            reviewLikeRepository.delete(existingLike.get());
        } else {
            reviewLikeRepository.save(new ReviewLike(review, userEmail));
        }

        return toResponse(review, userEmail);
    }

    @Transactional(readOnly = true)
    public java.util.List<ReviewResponse> findByContent(UUID contentId, String currentUserEmail) {
        return toResponseBatch(
                reviewRepository.findByContentIdOrderByCreatedAtDesc(contentId), currentUserEmail);
    }

    @Transactional(readOnly = true)
    public java.util.List<ReviewResponse> findBySourceApp(String sourceApp, String currentUserEmail) {
        return toResponseBatch(
                reviewRepository.findByContentSourceAppOrderByCreatedAtDesc(sourceApp), currentUserEmail);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> findByContentPaged(UUID contentId, String currentUserEmail, Pageable pageable) {
        Page<Review> page = reviewRepository.findByContentIdOrderByCreatedAtDesc(contentId, pageable);
        List<ReviewResponse> mapped = toResponseBatch(page.getContent(), currentUserEmail);
        return new org.springframework.data.domain.PageImpl<>(mapped, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> findBySourceAppPaged(String sourceApp, String currentUserEmail, Pageable pageable) {
        Page<Review> page = reviewRepository.findByContentSourceAppOrderByCreatedAtDesc(sourceApp, pageable);
        List<ReviewResponse> mapped = toResponseBatch(page.getContent(), currentUserEmail);
        return new org.springframework.data.domain.PageImpl<>(mapped, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReviewStatsResponse getMyStats(String userEmail) {
        long tapebeatReviews = reviewRepository.countByAuthorEmailAndContentSourceApp(userEmail, "tapebeat");
        long tapeflixReviews = reviewRepository.countByAuthorEmailAndContentSourceApp(userEmail, "tapeflix");

        List<Review> top = reviewRepository.findTopByAuthorEmailOrderByLikesDesc(
                userEmail, org.springframework.data.domain.PageRequest.of(0, 1));
        if (top.isEmpty()) {
            return new ReviewStatsResponse(tapebeatReviews, tapeflixReviews, null, null, 0);
        }
        Review mostLiked = top.get(0);
        long mostLikedCount = reviewLikeRepository.countByReviewId(mostLiked.getId());

        return new ReviewStatsResponse(
                tapebeatReviews,
                tapeflixReviews,
                mostLiked.getTitle(),
                mostLiked.getContent().getSourceApp(),
                mostLikedCount
        );
    }

    @Transactional
    public void deleteReview(UUID reviewId, String userEmail, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseña no encontrada"));

        if (!isAdmin && !review.getAuthorEmail().equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para eliminar esta reseña");
        }

        // Borrar likes y comentarios primero para no violar las FK.
        reviewLikeRepository.deleteByReviewId(review.getId());
        commentRepository.deleteByReviewId(review.getId());
        reviewRepository.delete(review);
    }

    private ReviewResponse toResponse(Review review, String currentUserEmail) {
        List<ReviewResponse> batch = toResponseBatch(List.of(review), currentUserEmail);
        return batch.get(0);
    }

    private List<ReviewResponse> toResponseBatch(List<Review> reviews, String currentUserEmail) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = reviews.stream().map(Review::getId).toList();

        Map<UUID, Long> likes = new HashMap<>();
        for (Object[] row : reviewLikeRepository.countGroupedByReviewIds(ids)) {
            likes.put((UUID) row[0], (Long) row[1]);
        }
        Map<UUID, Long> comments = new HashMap<>();
        for (Object[] row : commentRepository.countGroupedByReviewIds(ids)) {
            comments.put((UUID) row[0], (Long) row[1]);
        }
        Set<UUID> likedIds = Collections.emptySet();
        if (currentUserEmail != null && !currentUserEmail.isBlank()) {
            likedIds = new HashSet<>(reviewLikeRepository.findLikedIdsByUser(ids, currentUserEmail));
        }
        boolean hasUser = currentUserEmail != null && !currentUserEmail.isBlank();

        Set<UUID> finalLikedIds = likedIds;
        return reviews.stream().map(review -> new ReviewResponse(
                review.getId(),
                review.getContent().getId(),
                review.getContent().getTitle(),
                review.getContent().getExternalId(),
                review.getContent().getSourceType(),
                review.getContent().getImageUrl(),
                review.getAuthorDisplayName(),
                review.getTitle(),
                review.getBody(),
                review.getRating(),
                review.getIsSpoiler() != null ? review.getIsSpoiler() : Boolean.FALSE,
                likes.getOrDefault(review.getId(), 0L),
                comments.getOrDefault(review.getId(), 0L),
                hasUser && finalLikedIds.contains(review.getId()),
                hasUser && review.getAuthorEmail().equalsIgnoreCase(currentUserEmail),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getLastEditedAt()
        )).toList();
    }

    /**
     * Edita una reseña propia (o cualquier reseña si es admin). Solo se puede
     * editar una vez cada {@code app.review.edit-cooldown-seconds} (429 si es
     * muy pronto) para evitar ediciones masivas. La primera edición siempre
     * está permitida (lastEditedAt null).
     */
    @Transactional
    public ReviewResponse updateReview(UUID reviewId, ReviewRequest request, String userEmail, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseña no encontrada"));

        // Verificar permisos: solo el autor o admin pueden editar
        if (!isAdmin && !review.getAuthorEmail().equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para editar esta reseña");
        }

        if (review.getLastEditedAt() != null) {
            // elapsed negativo (reloj desfasado) se trata como 0: espera el cooldown completo, nunca más.
            long elapsed = Math.max(0, Duration.between(review.getLastEditedAt(), Instant.now()).getSeconds());
            if (elapsed < editCooldownSeconds) {
                throw new TooManyAttemptsException(
                        "Podés volver a editar en " + (editCooldownSeconds - elapsed) + " segundos");
            }
        }

        // Bloquear (con aviso) si hay lenguaje no permitido en título o cuerpo.
        profanityFilterService.requireClean(request.title(), "El título");
        profanityFilterService.requireClean(request.body(), "La reseña");

        validateRating(request.rating());

        // Actualizar campos
        review.setTitle(request.title().trim());
        review.setBody(request.body().trim());
        review.setRating(request.rating());
        review.setIsSpoiler(request.isSpoiler() != null ? request.isSpoiler() : Boolean.FALSE);
        review.setLastEditedAt(Instant.now());

        reviewRepository.save(review);
        return toResponse(review, userEmail);
    }
}
