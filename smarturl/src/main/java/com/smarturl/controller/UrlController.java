package com.smarturl.controller;

import com.smarturl.dto.AnalyticsResponse;
import com.smarturl.dto.ShortenRequest;
import com.smarturl.dto.ShortenResponse;
import com.smarturl.model.Url;
import com.smarturl.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * POST /api/v1/urls/shorten
     * Shortens a URL — public endpoint, no auth required
     */
    @PostMapping("/api/v1/urls/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @RequestBody ShortenRequest request) {
        try {
            ShortenResponse response = urlService.shortenUrl(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /r/{shortCode}
     * Redirects to original URL — the core feature
     * Returns 302 (temporary redirect) so analytics are always captured
     */
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            String referer   = request.getHeader("Referer");
            String ipAddress = request.getRemoteAddr();

            String originalUrl = urlService.getOriginalUrl(
                    shortCode, userAgent, referer, ipAddress);

            // 302 Found — temporary redirect, browser always calls us again
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(originalUrl))
                    .build();

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // URL expired
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
    }

    /**
     * GET /api/v1/urls/{shortCode}/analytics
     * Returns full analytics for a short URL
     */
    @GetMapping("/api/v1/urls/{shortCode}/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable String shortCode) {
        try {
            return ResponseEntity.ok(urlService.getAnalytics(shortCode));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/v1/urls/user/{userId}
     * Returns all URLs for a user
     */
    @GetMapping("/api/v1/urls/user/{userId}")
    public ResponseEntity<List<Url>> getUserUrls(@PathVariable String userId) {
        return ResponseEntity.ok(urlService.getUserUrls(userId));
    }

    /**
     * GET /api/v1/urls/health
     * Health check
     */
    @GetMapping("/api/v1/urls/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("SmartURL service is running");
    }
}