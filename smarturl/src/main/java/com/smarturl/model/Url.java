package com.smarturl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity             // Tells JPA: map this class to a database table
@Table(name = "urls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment ID
    private Long id;

    // The 6-character short code — e.g. "dnh75K"
    // Nullable on first insert — we set it after getting the auto-generated ID
    @Column(unique = true, length = 10)
    private String shortCode;

    // The original long URL
    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    // NULL for anonymous users
    @Column
    private String userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // NULL for registered users — anonymous URLs expire after 30 days
    @Column
    private LocalDateTime expiresAt;

    // Did ML flag this URL as malicious?
    @Column(nullable = false)
    private boolean malicious = false;

    // How confident was the ML model? 0.0 - 1.0
    @Column
    private Double mlConfidence;

    // Total click count — cached here for fast display
    @Column(nullable = false)
    private Long clickCount = 0L;

    @PrePersist // Runs automatically before saving to database
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}