package com.smarturl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "clicks", indexes = {
        // Index on shortCode for fast analytics queries
        // Without this, every analytics query scans the entire table
        @Index(name = "idx_clicks_short_code", columnList = "shortCode"),
        @Index(name = "idx_clicks_clicked_at", columnList = "clickedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which short URL was clicked
    @Column(nullable = false, length = 10)
    private String shortCode;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    // Where the click came from geographically
    @Column(length = 100)
    private String country;

    // Mobile, Desktop, or Tablet
    @Column(length = 50)
    private String deviceType;

    // Chrome, Firefox, Safari etc.
    @Column(length = 100)
    private String browser;

    // What site referred the click — e.g. "twitter.com"
    @Column(columnDefinition = "TEXT")
    private String referer;

    // Raw IP — used for geo lookup, not exposed via API
    @Column(length = 50)
    private String ipAddress;

    @PrePersist
    public void prePersist() {
        clickedAt = LocalDateTime.now();
    }
}