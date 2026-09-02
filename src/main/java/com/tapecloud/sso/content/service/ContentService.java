package com.tapecloud.sso.content.service;

import com.tapecloud.sso.content.dto.CommentRequest;
import com.tapecloud.sso.content.dto.ContentRequest;
import com.tapecloud.sso.content.dto.ReviewRequest;
import com.tapecloud.sso.content.entity.Comment;
import com.tapecloud.sso.content.entity.MediaItem;
import com.tapecloud.sso.content.entity.Review;
import com.tapecloud.sso.content.repository.CommentRepository;
import com.tapecloud.sso.content.repository.MediaItemRepository;
import com.tapecloud.sso.content.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ContentService {

    private final MediaItemRepository contentRepository;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;

    public ContentService(MediaItemRepository contentRepository, ReviewRepository reviewRepository, CommentRepository commentRepository) {
        this.contentRepository = contentRepository;
        this.reviewRepository = reviewRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listContent(String app) {
        return contentRepository.findByAppOrderByTitleAsc(com.tapecloud.sso.content.entity.ContentApp.valueOf(app.toUpperCase()))
                .stream().map(this::contentView).toList();
    }

    @Transactional
    public Map<String, Object> createContent(ContentRequest request, String authorEmail) {
        String requestedTitle = request.title().trim();
        boolean duplicate = contentRepository.findByAppOrderByTitleAsc(request.app())
                .stream()
                .anyMatch(existing -> requestedTitle.toLowerCase(Locale.ROOT).contains(existing.getTitle().toLowerCase(Locale.ROOT)));
        if (duplicate) {
            throw new IllegalArgumentException("Ya existe contenido relacionado con ese título");
        }
        MediaItem content = contentRepository.save(new MediaItem(request.app(), request.type().trim(), requestedTitle, request.description().trim(), request.genre().trim(), request.releaseYear(), authorEmail));
        return contentView(content);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listReviews(UUID contentId) {
        getContent(contentId);
        return reviewRepository.findByContentIdOrderByCreatedAtDesc(contentId).stream().map(this::reviewView).toList();
    }

    @Transactional
    public Map<String, Object> createReview(UUID contentId, ReviewRequest request, String authorEmail) {
        MediaItem content = getContent(contentId);
        return reviewView(reviewRepository.save(new Review(content, authorEmail, request.title().trim(), request.body().trim(), request.rating())));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listComments(UUID reviewId) {
        getReview(reviewId);
        return commentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId).stream().map(this::commentView).toList();
    }

    @Transactional
    public Map<String, Object> createComment(UUID reviewId, CommentRequest request, String authorEmail) {
        return commentView(commentRepository.save(new Comment(getReview(reviewId), authorEmail, request.body().trim())));
    }

    private MediaItem getContent(UUID id) {
        return contentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Contenido no encontrado"));
    }

    private Review getReview(UUID id) {
        return reviewRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));
    }

    private Map<String, Object> contentView(MediaItem content) {
        Map<String, Object> view = new java.util.HashMap<>();
        view.put("id", content.getId());
        view.put("app", content.getApp());
        view.put("type", content.getType());
        view.put("title", content.getTitle());
        view.put("description", content.getDescription());
        view.put("genre", content.getGenre());
        view.put("releaseYear", content.getReleaseYear());
        view.put("createdBy", content.getCreatedBy());
        return view;
    }

    private Map<String, Object> reviewView(Review review) {
        return Map.of("id", review.getId(), "contentId", review.getContent().getId(), "authorEmail", review.getAuthorEmail(), "title", review.getTitle(), "body", review.getBody(), "rating", review.getRating(), "createdAt", review.getCreatedAt());
    }

    private Map<String, Object> commentView(Comment comment) {
        return Map.of("id", comment.getId(), "reviewId", comment.getReview().getId(), "authorEmail", comment.getAuthorEmail(), "body", comment.getBody(), "createdAt", comment.getCreatedAt());
    }
}
