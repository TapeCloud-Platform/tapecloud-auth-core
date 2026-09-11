package com.tapecloud.auth.review.entity;

import com.tapecloud.auth.content.entity.ContentItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
    @JoinColumn(name = "content_id", nullable = false)
    private ContentItem content;

    @Column(nullable = false, length = 320)
    private String authorEmail;

    @Column(nullable = false, length = 120)
    private String authorDisplayName;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Review() {
    }

    public Review(ContentItem content, String authorEmail, String authorDisplayName, String title, String body, Integer rating) {
        this.content = content;
        this.authorEmail = authorEmail;
        this.authorDisplayName = authorDisplayName;
        this.title = title;
        this.body = body;
        this.rating = rating;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ContentItem getContent() { return content; }
    public String getAuthorEmail() { return authorEmail; }
    public String getAuthorDisplayName() { return authorDisplayName; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Integer getRating() { return rating; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
