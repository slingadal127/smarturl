package com.smarturl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private String shortCode;
    private String originalUrl;
    private long totalClicks;
    private String createdAt;
    private String expiresAt;

    // Top countries — e.g. {"USA": 450, "India": 230, "UK": 180}
    private Map<String, Long> clicksByCountry;

    // Device breakdown — e.g. {"Desktop": 600, "Mobile": 300, "Tablet": 100}
    private Map<String, Long> clicksByDevice;

    // Referrer breakdown — e.g. {"twitter.com": 400, "Direct": 300}
    private Map<String, Long> clicksByReferer;

    // Time series — e.g. [{"date": "2024-01-01", "clicks": 45}, ...]
    private List<Map<String, Object>> clicksOverTime;
}