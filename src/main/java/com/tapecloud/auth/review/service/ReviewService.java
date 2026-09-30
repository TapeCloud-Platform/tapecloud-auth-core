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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final CommentRepository commentRepository;
    private final ContentItemRepository contentItemRepository;
    private final AppUserRepository userRepository;

    @Value("${bad-words.base-file:bad-words-base.txt}")
    private String badWordsBaseFile;

    @Value("${bad-words.custom-file:bad-words-custom.txt}")
    private String badWordsCustomFile;

    private final Set<String> PROFANITY_WORDS = new HashSet<>();

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

    @PostConstruct
    public void initProfanityList() {
        loadDictionaryFromResource(badWordsBaseFile);
        loadDictionaryFromResource(badWordsCustomFile);
    }

    @PreDestroy
    public void cleanup() {
        PROFANITY_WORDS.clear();
    }

    private void loadDictionaryFromResource(String fileName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        PROFANITY_WORDS.add(trimmed.toLowerCase());
                    }
                }
            }
        } catch (IOException e) {
            // Si el archivo no existe, continuamos con la lista vacía o mínima
            // para no romper la aplicación al inicio
        }
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

        // Filtrar palabrotas usando el diccionario cargado
        String filteredTitle = filterProfanity(request.title().trim());
        String filteredBody = filterProfanity(request.body().trim());

        // Si después del filtrado el título o cuerpo están vacíos, lanzar error
        if (filteredTitle.isBlank() || filteredBody.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reseña contiene palabras prohibidas y no puede publicarse");
        }

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail().split("@")[0];

        validateRating(request.rating());

        Review review = new Review(
                content,
                user.getEmail(),
                displayName,
                filteredTitle,
                filteredBody,
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
        return reviewRepository.findByContentIdOrderByCreatedAtDesc(contentId).stream()
                .map(review -> toResponse(review, currentUserEmail))
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ReviewResponse> findBySourceApp(String sourceApp, String currentUserEmail) {
        return reviewRepository.findByContentSourceAppOrderByCreatedAtDesc(sourceApp).stream()
                .map(review -> toResponse(review, currentUserEmail))
                .toList();
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

        boolean ownedByCurrentUser = currentUserEmail != null &&
                !currentUserEmail.isBlank() &&
                review.getAuthorEmail().equalsIgnoreCase(currentUserEmail);

        return new ReviewResponse(
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
                likesCount,
                commentsCount,
                likedByCurrentUser,
                ownedByCurrentUser,
                review.getCreatedAt()
        );
    }

    @Transactional
    public ReviewResponse updateReview(UUID reviewId, ReviewRequest request, String userEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reseña no encontrada"));

        // Verificar permisos: solo el autor o admin pueden editar
        if (!review.getAuthorEmail().equalsIgnoreCase(userEmail) && !isAdmin(review, userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para editar esta reseña");
        }

        // Filtrar palabrotas del título y cuerpo
        String filteredTitle = filterProfanity(request.title().trim());
        String filteredBody = filterProfanity(request.body().trim());

        // Si después del filtrado el título o cuerpo están vacíos, lanzar error
        if (filteredTitle.isBlank() || filteredBody.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reseña contiene palabras prohibidas y no puede publicarse");
        }

        validateRating(request.rating());

        // Actualizar campos
        review.setTitle(filteredTitle);
        review.setBody(filteredBody);
        review.setRating(request.rating());
        review.setIsSpoiler(request.isSpoiler() != null ? request.isSpoiler() : Boolean.FALSE);

        reviewRepository.save(review);
        return toResponse(review, userEmail);
    }

    private boolean isAdmin(Review review, String userEmail) {
        // Este método sería implementado según tu lógica de roles
        // Por simplicidad, retornamos false; los checks de email bastan
        return false;
    }

    /** Filtrado de palabrotas: reemplaza palabras prohibidas por asteríscos del mismo largo. Insensible a mayúsculas/minúsculas. */
    private String filterProfanity(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String result = text;
        for (String word : PROFANITY_WORDS) {
            // Escape dots and other regex special chars in the word
            String escapedWord = word.replace(".", "\\\\.")
                    .replace("^", "\\\\^")
                    .replace("$", "\\\\$")
                    .replace("|", "\\\\|")
                    .replace("?", "\\\\?")
                    .replace("*", "\\\\*")
                    .replace("+", "\\\\+")
                    .replace("(", "\\\\(")
                    .replace("\"", "\\\\\"");
            result = result.replaceAll("(?i)" + escapedWord, "*".repeat(word.length()));
        }
        return result;
    }
}
