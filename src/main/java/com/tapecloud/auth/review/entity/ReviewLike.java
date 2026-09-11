package com.tapecloud.auth.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "review_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_email"})
)
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "user_email", nullable = false, length = 320)
    private String userEmail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ReviewLike() {
    }

    public ReviewLike(Review review, String userEmail) {
        this.review = review;
        this.userEmail = userEmail;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Review getReview() { return review; }
    public String getUserEmail() { return userEmail; }
    public Instant getCreatedAt() { return createdAt; }
}
