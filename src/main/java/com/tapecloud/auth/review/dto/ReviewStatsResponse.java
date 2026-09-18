package com.tapecloud.auth.review.dto;

public record ReviewStatsResponse(
        long tapebeatReviews,
        long tapeflixReviews,
        String mostLikedReviewTitle,
        String mostLikedReviewSourceApp,
        long mostLikedReviewLikes
) {
}
