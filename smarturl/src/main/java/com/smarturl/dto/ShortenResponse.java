package com.smarturl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenResponse {

    // The 6-character short code
    private String shortCode;

    // Full short URL — e.g. "http://localhost:8080/r/dnh75K"
    private String shortUrl;

    // The original URL
    private String originalUrl;

    // Was the URL safe according to ML classifier?
    private boolean safe;

    // ML confidence score 0.0 - 1.0
    private double mlConfidence;

    // Human readable safety message
    private String safetyMessage;

    // When does this URL expire? null = never (registered users)
    private String expiresAt;
}