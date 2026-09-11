package com.tapecloud.auth.review.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID reviewId,
        String authorEmail,
        String authorDisplayName,
        String body,
        Instant createdAt
) {
}
