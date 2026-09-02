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
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private MediaItem content;

    @Column(nullable = false, length = 320)
    private String authorEmail;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 5000)
    private String body;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Review() {
    }

    public Review(MediaItem content, String authorEmail, String title, String body, Integer rating) {
        this.content = content;
        this.authorEmail = authorEmail;
        this.title = title;
        this.body = body;
        this.rating = rating;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public MediaItem getContent() { return content; }
    public String getAuthorEmail() { return authorEmail; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Integer getRating() { return rating; }
    public Instant getCreatedAt() { return createdAt; }
}
