package com.tapecloud.sso.content.api;

import com.tapecloud.sso.content.dto.CommentRequest;
import com.tapecloud.sso.content.dto.ContentRequest;
import com.tapecloud.sso.content.dto.ReviewRequest;
import com.tapecloud.sso.content.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/content")
    public List<Map<String, Object>> listContent(@RequestParam String app) {
        return contentService.listContent(app);
    }

    @PostMapping("/content")
    public ResponseEntity<Map<String, Object>> createContent(@Valid @RequestBody ContentRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.createContent(request, authentication.getName()));
    }

    @GetMapping("/content/{contentId}/reviews")
    public List<Map<String, Object>> listReviews(@PathVariable UUID contentId) {
        return contentService.listReviews(contentId);
    }

    @PostMapping("/content/{contentId}/reviews")
    public ResponseEntity<Map<String, Object>> createReview(@PathVariable UUID contentId, @Valid @RequestBody ReviewRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.createReview(contentId, request, authentication.getName()));
    }

    @GetMapping("/reviews/{reviewId}/comments")
    public List<Map<String, Object>> listComments(@PathVariable UUID reviewId) {
        return contentService.listComments(reviewId);
    }

    @PostMapping("/reviews/{reviewId}/comments")
    public ResponseEntity<Map<String, Object>> createComment(@PathVariable UUID reviewId, @Valid @RequestBody CommentRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.createComment(reviewId, request, authentication.getName()));
    }
}
