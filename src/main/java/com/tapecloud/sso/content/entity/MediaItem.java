package com.tapecloud.sso.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "media_items")
public class MediaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentApp app;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Column(nullable = false, length = 120)
    private String genre;

    private Integer releaseYear;

    @Column(nullable = false, length = 320)
    private String createdBy;

    protected MediaItem() {
    }

    public MediaItem(ContentApp app, String type, String title, String description, String genre, Integer releaseYear, String createdBy) {
        this.app = app;
        this.type = type;
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.releaseYear = releaseYear;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public ContentApp getApp() { return app; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getGenre() { return genre; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getCreatedBy() { return createdBy; }
}
