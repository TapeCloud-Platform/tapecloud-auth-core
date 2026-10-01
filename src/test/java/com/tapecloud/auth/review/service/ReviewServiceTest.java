package com.tapecloud.auth.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.repository.ContentItemRepository;
import com.tapecloud.auth.exception.TooManyAttemptsException;
import com.tapecloud.auth.moderation.ProfanityFilterService;
import com.tapecloud.auth.review.dto.ReviewRequest;
import com.tapecloud.auth.review.dto.ReviewResponse;
import com.tapecloud.auth.review.entity.Review;
import com.tapecloud.auth.review.repository.CommentRepository;
import com.tapecloud.auth.review.repository.ReviewLikeRepository;
import com.tapecloud.auth.review.repository.ReviewRepository;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.repository.AppUserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private ReviewLikeRepository likeRepository;
    private CommentRepository commentRepository;
    private ContentItemRepository contentRepository;
    private AppUserRepository userRepository;
    private ProfanityFilterService profanity;
    private ReviewService service;

    @BeforeEach
    void setUp() throws Exception {
        reviewRepository = mock(ReviewRepository.class);
        likeRepository = mock(ReviewLikeRepository.class);
        commentRepository = mock(CommentRepository.class);
        contentRepository = mock(ContentItemRepository.class);
        userRepository = mock(AppUserRepository.class);
        profanity = mock(ProfanityFilterService.class);
        service = new ReviewService(reviewRepository, likeRepository, commentRepository,
                contentRepository, userRepository, profanity);
        Field cooldown = ReviewService.class.getDeclaredField("editCooldownSeconds");
        cooldown.setAccessible(true);
        cooldown.setLong(service, 30L);
    }

    private ContentItem content() {
        return new ContentItem("tapeflix", "movie", "550", "Fight Club",
                "desc", "http://img", LocalDate.of(1999, 10, 15), "Drama");
    }

    private AppUser user() {
        return new AppUser("juan@mail.com", "secret", "juan");
    }

    @Test
    void duplicateReviewConflicts() {
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content()));
        when(userRepository.findByEmailIgnoreCase("juan@mail.com")).thenReturn(Optional.of(user()));
        when(reviewRepository.existsByContentIdAndAuthorEmailIgnoreCase(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.createReview(contentId,
                new ReviewRequest("t", "b", 4.5, false), "juan@mail.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ya publicaste");
    }

    @Test
    void ratingMustBeHalfIncrements() {
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content()));
        when(userRepository.findByEmailIgnoreCase("juan@mail.com")).thenReturn(Optional.of(user()));
        when(reviewRepository.existsByContentIdAndAuthorEmailIgnoreCase(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.createReview(contentId,
                new ReviewRequest("t", "b", 4.3, false), "juan@mail.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("incrementos de 0.5");
    }

    @Test
    void firstEditAlwaysAllowedThenCooldown() {
        Review review = new Review(content(), "juan@mail.com", "juan", "t", "b", 4.0, false);
        UUID id = UUID.randomUUID();
        when(reviewRepository.findById(id)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(likeRepository.countGroupedByReviewIds(any())).thenReturn(List.of());
        when(commentRepository.countGroupedByReviewIds(any())).thenReturn(List.of());

        // lastEditedAt null -> primera edición permitida
        ReviewResponse r1 = service.updateReview(id,
                new ReviewRequest("t2", "b2", 4.5, false), "juan@mail.com", false);
        assertThat(r1.title()).isEqualTo("t2");
        assertThat(review.getLastEditedAt()).isNotNull();

        // segunda edición inmediata -> 429
        assertThatThrownBy(() -> service.updateReview(id,
                new ReviewRequest("t3", "b3", 5.0, false), "juan@mail.com", false))
                .isInstanceOf(TooManyAttemptsException.class);

        // reloj desfasado (lastEditedAt en futuro) -> trata como recién editada, no conteo absurdo
        review.setLastEditedAt(Instant.now().plusSeconds(3600));
        assertThatThrownBy(() -> service.updateReview(id,
                new ReviewRequest("t4", "b4", 5.0, false), "juan@mail.com", false))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessageContaining("30 segundos");
    }

    @Test
    void pagedListUsesBatchCounts() {
        Review review = new Review(content(), "juan@mail.com", "juan", "t", "b", 5.0, false);
        var page = new PageImpl<>(List.of(review));
        when(reviewRepository.findByContentSourceAppOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq("tapeflix"), any()))
                .thenReturn(page);
        when(likeRepository.countGroupedByReviewIds(any())).thenReturn(List.of());
        when(commentRepository.countGroupedByReviewIds(any())).thenReturn(List.of());
        when(likeRepository.findLikedIdsByUser(any(), any())).thenReturn(java.util.Set.of());

        var result = service.findBySourceAppPaged("tapeflix", "juan@mail.com", PageRequest.of(0, 20));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
