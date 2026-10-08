package com.tapecloud.auth.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.exception.TooManyAttemptsException;
import com.tapecloud.auth.moderation.ProfanityFilterService;
import com.tapecloud.auth.moderation.ProfanityProperties;
import com.tapecloud.auth.review.dto.CommentRequest;
import com.tapecloud.auth.review.entity.Comment;
import com.tapecloud.auth.review.entity.Review;
import com.tapecloud.auth.review.repository.CommentRepository;
import com.tapecloud.auth.review.repository.ReviewRepository;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.repository.AppUserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentServiceTest {

    private CommentRepository commentRepository;
    private ReviewRepository reviewRepository;
    private AppUserRepository userRepository;
    private CommentService service;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository = mock(CommentRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        userRepository = mock(AppUserRepository.class);
        // Instancia real (no mock): Mockito/ByteBuddy del proyecto no soporta
        // el Java 25 de este entorno para mockear clases.
        ProfanityFilterService profanity = new ProfanityFilterService(new ProfanityProperties());
        profanity.load();
        service = new CommentService(commentRepository, reviewRepository, userRepository, profanity);
        Field cooldown = CommentService.class.getDeclaredField("commentCooldownSeconds");
        cooldown.setAccessible(true);
        cooldown.setLong(service, 30L);

        ContentItem content = new ContentItem("tapeflix", "movie", "550", "Fight Club",
                "desc", "http://img", LocalDate.of(1999, 10, 15), "Drama");
        Review review = new Review(content, "autor@mail.com", "autor", "t", "b", 4.0, false);
        when(reviewRepository.findById(any())).thenReturn(Optional.of(review));
        when(userRepository.findByEmailIgnoreCase("juan@mail.com"))
                .thenReturn(Optional.of(new AppUser("juan@mail.com", "secret", "juan")));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Comment previousComment(Instant createdAt) throws Exception {
        ContentItem content = new ContentItem("tapeflix", "movie", "550", "Fight Club",
                "desc", "http://img", LocalDate.of(1999, 10, 15), "Drama");
        Review review = new Review(content, "autor@mail.com", "autor", "t", "b", 4.0, false);
        Comment comment = new Comment(review, "juan@mail.com", "juan", "viejo");
        Field field = Comment.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(comment, createdAt);
        return comment;
    }

    @Test
    void firstCommentAlwaysAllowed() {
        when(commentRepository.findTopByAuthorEmailIgnoreCaseOrderByCreatedAtDesc("juan@mail.com"))
                .thenReturn(Optional.empty());

        var response = service.createComment(UUID.randomUUID(), new CommentRequest("hola"), "juan@mail.com");
        assertThat(response.body()).isEqualTo("hola");
    }

    @Test
    void commentWithinCooldownIsRejected() throws Exception {
        when(commentRepository.findTopByAuthorEmailIgnoreCaseOrderByCreatedAtDesc("juan@mail.com"))
                .thenReturn(Optional.of(previousComment(Instant.now())));

        assertThatThrownBy(() -> service.createComment(UUID.randomUUID(), new CommentRequest("hola"), "juan@mail.com"))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessageContaining("30 segundos");
    }

    @Test
    void commentAfterCooldownIsAllowed() throws Exception {
        when(commentRepository.findTopByAuthorEmailIgnoreCaseOrderByCreatedAtDesc("juan@mail.com"))
                .thenReturn(Optional.of(previousComment(Instant.now().minusSeconds(60))));

        var response = service.createComment(UUID.randomUUID(), new CommentRequest("hola"), "juan@mail.com");
        assertThat(response.body()).isEqualTo("hola");
    }
}
