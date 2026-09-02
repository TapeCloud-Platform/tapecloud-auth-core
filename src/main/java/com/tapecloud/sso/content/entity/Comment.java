package com.tapecloud.sso.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Review review;

    @Column(nullable = false, length = 320)
    private String authorEmail;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Comment() {
    }

    public Comment(Review review, String authorEmail, String body) {
        this.review = review;
        this.authorEmail = authorEmail;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Review getReview() { return review; }
    public String getAuthorEmail() { return authorEmail; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
