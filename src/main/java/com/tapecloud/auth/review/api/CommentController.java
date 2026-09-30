package com.tapecloud.auth.review.api;

import com.tapecloud.auth.review.dto.CommentRequest;
import com.tapecloud.auth.review.dto.CommentResponse;
import com.tapecloud.auth.review.service.CommentService;
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
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@RequestParam UUID reviewId, Authentication authentication) {
        String currentUserEmail = (authentication != null) ? authentication.getName() : null;
        return commentService.findByReview(reviewId, currentUserEmail);
    }

    @PostMapping("/review/{reviewId}")
    public ResponseEntity<CommentResponse> create(
            @PathVariable UUID reviewId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        CommentResponse created = commentService.createComment(reviewId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID commentId, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        commentService.deleteComment(commentId, authentication.getName(), isAdmin);
    }
}
