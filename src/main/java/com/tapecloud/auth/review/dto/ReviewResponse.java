package com.tapecloud.auth.review.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID contentId,
        String contentTitle,
        String authorEmail,
        String authorDisplayName,
        String title,
        String body,
        Integer rating,
        long likesCount,
        long commentsCount,
        boolean likedByCurrentUser,
        Instant createdAt
) {
}

