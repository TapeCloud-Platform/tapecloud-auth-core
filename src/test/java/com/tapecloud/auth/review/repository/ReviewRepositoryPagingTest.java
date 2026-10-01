package com.tapecloud.auth.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.repository.ContentItemRepository;
import com.tapecloud.auth.review.entity.Review;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ReviewRepositoryPagingTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ContentItemRepository contentRepository;

    @Test
    void pagedFindByContent() {
        ContentItem content = contentRepository.save(new ContentItem("tapeflix", "movie", "550",
                "Fight Club", "desc", "", LocalDate.of(1999, 10, 15), "Drama"));
        for (int i = 0; i < 5; i++) {
            reviewRepository.save(new Review(content, "u" + i + "@mail.com", "u" + i,
                    "t" + i, "body " + i, 4.0, false));
        }

        Page<Review> p0 = reviewRepository.findByContentIdOrderByCreatedAtDesc(
                content.getId(), PageRequest.of(0, 2));
        Page<Review> p1 = reviewRepository.findByContentIdOrderByCreatedAtDesc(
                content.getId(), PageRequest.of(1, 2));
        Page<Review> p2 = reviewRepository.findByContentIdOrderByCreatedAtDesc(
                content.getId(), PageRequest.of(2, 2));

        assertThat(p0.getTotalElements()).isEqualTo(5);
        assertThat(p0.getContent()).hasSize(2);
        assertThat(p1.getContent()).hasSize(2);
        assertThat(p2.getContent()).hasSize(1);
    }

    @Test
    void topByAuthorOrdersByLikes() {
        ContentItem content = contentRepository.save(new ContentItem("tapeflix", "movie", "551",
                "Se7en", "desc", "", LocalDate.of(1995, 9, 22), "Drama"));
        Review r = reviewRepository.save(new Review(content, "juan@mail.com", "juan",
                "t", "b", 5.0, false));

        var top = reviewRepository.findTopByAuthorEmailOrderByLikesDesc(
                "JUAN@mail.com", PageRequest.of(0, 1));
        assertThat(top).hasSize(1);
        assertThat(top.get(0).getId()).isEqualTo(r.getId());
    }
}
