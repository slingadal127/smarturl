package com.smarturl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenRequest {

    // The long URL to shorten — e.g. "https://www.example.com/very/long/path"
    private String originalUrl;

    // Optional — if provided, URL is linked to this user's account
    // If null, URL is anonymous and expires after 30 days
    private String userId;
}