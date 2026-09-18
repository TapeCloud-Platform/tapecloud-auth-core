package com.tapecloud.auth.review.service;

import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.repository.ContentItemRepository;
import com.tapecloud.auth.review.dto.ReviewRequest;
import com.tapecloud.auth.review.dto.ReviewResponse;
import com.tapecloud.auth.review.dto.ReviewStatsResponse;
import com.tapecloud.auth.review.entity.Review;
import com.tapecloud.auth.review.entity.ReviewLike;
import com.tapecloud.auth.review.repository.CommentRepository;
import com.tapecloud.auth.review.repository.ReviewLikeRepository;
import com.tapecloud.auth.review.repository.ReviewRepository;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewLikeRepository reviewLikeRepository,
            CommentRepository commentRepository,
            ContentItemRepository contentItemRepository,
            AppUserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.commentRepository = commentRepository;
        this.contentItemRepository = contentItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findByContent(UUID contentId, String currentUserEmail) {
        return reviewRepository.findByContentIdOrderByCreatedAtDesc(contentId).stream()
                .map(r -> toResponse(r, currentUserEmail))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findBySourceApp(String sourceApp, String currentUserEmail) {
        return reviewRepository.findByContentSourceAppOrderByCreatedAtDesc(sourceApp).stream()
                .map(r -> toResponse(r, currentUserEmail))
                .toList();
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

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail().split("@")[0];

        Review review = new Review(
                content,
                user.getEmail(),
                displayName,
                request.title().trim(),
                request.body().trim(),
                request.rating()
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
    public ReviewStatsResponse getMyStats(String userEmail) {
        long tapebeatReviews = reviewRepository.countByAuthorEmailAndContentSourceApp(userEmail, "tapebeat");
        long tapeflixReviews = reviewRepository.countByAuthorEmailAndContentSourceApp(userEmail, "tapeflix");

        Review mostLiked = null;
        long mostLikedCount = 0;
        for (Review review : reviewRepository.findByAuthorEmailOrderByCreatedAtDesc(userEmail)) {
            long likes = reviewLikeRepository.countByReviewId(review.getId());
            if (mostLiked == null || likes > mostLikedCount) {
                mostLiked = review;
                mostLikedCount = likes;
            }
        }

        return new ReviewStatsResponse(
                tapebeatReviews,
                tapeflixReviews,
                mostLiked != null ? mostLiked.getTitle() : null,
                mostLiked != null ? mostLiked.getContent().getSourceApp() : null,
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

        reviewRepository.delete(review);
    }

    private ReviewResponse toResponse(Review review, String currentUserEmail) {
        long likesCount = reviewLikeRepository.countByReviewId(review.getId());
        long commentsCount = commentRepository.countByReviewId(review.getId());
        boolean likedByCurrentUser = currentUserEmail != null &&
                !currentUserEmail.isBlank() &&
                reviewLikeRepository.existsByReviewIdAndUserEmailIgnoreCase(review.getId(), currentUserEmail);

        return new ReviewResponse(
                review.getId(),
                review.getContent().getId(),
                review.getContent().getTitle(),
                review.getAuthorEmail(),
                review.getAuthorDisplayName(),
                review.getTitle(),
                review.getBody(),
                review.getRating(),
                likesCount,
                commentsCount,
                likedByCurrentUser,
                review.getCreatedAt()
        );
    }
}

