package com.tapecloud.auth.review.api;

import com.tapecloud.auth.review.dto.ReviewRequest;
import com.tapecloud.auth.review.dto.ReviewResponse;
import com.tapecloud.auth.review.dto.ReviewStatsResponse;
import com.tapecloud.auth.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ReviewResponse> list(
            @RequestParam(required = false) String sourceApp,
            @RequestParam(required = false) UUID contentId,
            Authentication authentication
    ) {
        String currentUserEmail = (authentication != null) ? authentication.getName() : null;
        if (contentId != null) {
            return reviewService.findByContent(contentId, currentUserEmail);
        }
        if (sourceApp != null && !sourceApp.isBlank()) {
            return reviewService.findBySourceApp(sourceApp, currentUserEmail);
        }
        return reviewService.findBySourceApp("tapeflix", currentUserEmail);
    }

    @GetMapping("/me/stats")
    public ReviewStatsResponse myStats(Authentication authentication) {
        return reviewService.getMyStats(authentication.getName());
    }

    @PostMapping("/content/{contentId}")
    public ResponseEntity<ReviewResponse> create(
            @PathVariable UUID contentId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        ReviewResponse created = reviewService.createReview(contentId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{reviewId}/like")
    public ResponseEntity<ReviewResponse> toggleLike(
            @PathVariable UUID reviewId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reviewService.toggleLike(reviewId, authentication.getName()));
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID reviewId, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        reviewService.deleteReview(reviewId, authentication.getName(), isAdmin);
    }
}

