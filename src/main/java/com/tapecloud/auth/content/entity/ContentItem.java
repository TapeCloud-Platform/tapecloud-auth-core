package com.tapecloud.auth.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "content_items")
public class ContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String sourceApp;

    @Column(nullable = false, length = 30)
    private String sourceType;

    @Column(nullable = false, length = 120)
    private String externalId;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 250)
    private String genre;

    private LocalDate releaseDate;


    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ContentItem() {
    }

    public ContentItem(String sourceApp, String sourceType, String externalId, String title,
                       String description, String imageUrl, LocalDate releaseDate, String genre) {
        this.sourceApp = sourceApp;
        this.sourceType = sourceType;
        this.externalId = externalId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.releaseDate = releaseDate;
        this.genre = genre;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSourceApp() { return sourceApp; }
    public String getSourceType() { return sourceType; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getGenre() { return genre; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateDetails(String title, String description, String imageUrl, LocalDate releaseDate, String genre) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.releaseDate = releaseDate;
        this.genre = genre;
    }
}
