package com.smarturl.service;

import com.smarturl.dto.AnalyticsResponse;
import com.smarturl.dto.ShortenRequest;
import com.smarturl.dto.ShortenResponse;
import com.smarturl.model.Click;
import com.smarturl.model.Url;
import com.smarturl.repository.ClickRepository;
import com.smarturl.repository.UrlRepository;
import com.smarturl.util.Base62Encoder;
import com.smarturl.util.UserAgentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final ClickRepository clickRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final Base62Encoder base62Encoder;
    private final UserAgentParser userAgentParser;
    private final MlClassifierClient mlClassifierClient;


    private static final String REDIS_PREFIX = "url:";
    private static final long REDIS_TTL_HOURS = 24;
    private static final String BASE_URL = "http://localhost:8082/r/";


    public UrlService(UrlRepository urlRepository,
                      ClickRepository clickRepository,
                      RedisTemplate<String, String> redisTemplate,
                      Base62Encoder base62Encoder,
                      UserAgentParser userAgentParser,
                      MlClassifierClient mlClassifierClient) {
        this.urlRepository = urlRepository;
        this.clickRepository = clickRepository;
        this.redisTemplate = redisTemplate;
        this.base62Encoder = base62Encoder;
        this.userAgentParser = userAgentParser;
        this.mlClassifierClient = mlClassifierClient;
    }

    /**
     * Main method — shortens a URL
     * Flow: validate → ML check → generate code → save → cache → return
     */
    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request) {

        // Step 1: Basic URL validation
        String originalUrl = request.getOriginalUrl();
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
            originalUrl = "https://" + originalUrl;
        }

        // Step 2: Call ML service to screen the URL
        boolean isSafe = true;
        double mlConfidence = 0.0;
        String safetyMessage = "URL appears safe";

        Map<String, Object> mlResult = mlClassifierClient.classify(originalUrl);
        if (mlResult != null) {
            boolean isMalicious = (boolean) mlResult.get("is_malicious");
            mlConfidence = ((Number) mlResult.get("rf_confidence")).doubleValue();

            if (isMalicious) {
                // Block malicious URLs — don't shorten them
                return ShortenResponse.builder()
                        .shortCode(null)
                        .shortUrl(null)
                        .originalUrl(originalUrl)
                        .safe(false)
                        .mlConfidence(mlConfidence)
                        .safetyMessage("URL blocked — detected as malicious by AI classifier")
                        .build();
            }
        }

        // Step 3: Save to database — JPA auto-generates the ID
        Url url = Url.builder()
                .originalUrl(originalUrl)
                .userId(request.getUserId())
                .malicious(false)
                .mlConfidence(0.0)
                .clickCount(0L)
                // Anonymous URLs expire after 30 days
                .expiresAt(request.getUserId() == null
                        ? LocalDateTime.now().plusDays(30)
                        : null)
                .build();

        url = urlRepository.save(url);

        // Step 3: Generate short code from the auto-generated database ID
        // This is guaranteed collision-free — IDs are unique by definition
        String shortCode = base62Encoder.encode(url.getId());

        // Step 4: Update the record with the short code
        url.setShortCode(shortCode);
        urlRepository.save(url);

        // Step 5: Cache in Redis for fast redirects
        redisTemplate.opsForValue().set(
                REDIS_PREFIX + shortCode,
                originalUrl,
                REDIS_TTL_HOURS,
                TimeUnit.HOURS
        );

        log.info("Shortened URL: {} → {}", originalUrl, shortCode);

        return ShortenResponse.builder()
                .shortCode(shortCode)
                .shortUrl(BASE_URL + shortCode)
                .originalUrl(originalUrl)
                .safe(true)
                .mlConfidence(0.0)
                .safetyMessage("URL appears safe")
                .expiresAt(url.getExpiresAt() != null
                        ? url.getExpiresAt().format(DateTimeFormatter.ISO_DATE)
                        : null)
                .build();
    }

    /**
     * Redirect — called when someone clicks a short URL
     * Returns the original URL for redirect, records click asynchronously
     */
    public String getOriginalUrl(String shortCode,
                                 String userAgent,
                                 String referer,
                                 String ipAddress) {

        // Step 1: Check Redis cache first — sub-millisecond
        String cached = redisTemplate.opsForValue().get(REDIS_PREFIX + shortCode);
        if (cached != null) {
            // Refresh TTL on access
            redisTemplate.expire(REDIS_PREFIX + shortCode,
                    REDIS_TTL_HOURS, TimeUnit.HOURS);
            // Record click asynchronously — never block the redirect
            recordClick(shortCode, userAgent, referer, ipAddress);
            return cached;
        }

        // Step 2: Cache miss — query PostgreSQL
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new NoSuchElementException("Short URL not found"));

        // Step 3: Check expiry
        if (url.getExpiresAt() != null &&
                LocalDateTime.now().isAfter(url.getExpiresAt())) {
            throw new IllegalStateException("This short URL has expired");
        }

        // Step 4: Warm the cache
        redisTemplate.opsForValue().set(
                REDIS_PREFIX + shortCode,
                url.getOriginalUrl(),
                REDIS_TTL_HOURS,
                TimeUnit.HOURS
        );

        // Step 5: Record click asynchronously
        recordClick(shortCode, userAgent, referer, ipAddress);

        return url.getOriginalUrl();
    }

    /**
     * Records a click asynchronously — never blocks the redirect
     * @Async runs this on a background thread
     */
    @Async
    @Transactional
    public void recordClick(String shortCode,
                            String userAgent,
                            String referer,
                            String ipAddress) {
        try {
            Click click = Click.builder()
                    .shortCode(shortCode)
                    .deviceType(userAgentParser.getDeviceType(userAgent))
                    .browser(userAgentParser.getBrowser(userAgent))
                    .referer(userAgentParser.parseReferer(referer))
                    .ipAddress(ipAddress)
                    .country("Unknown") // Geo lookup comes later
                    .build();

            clickRepository.save(click);
            urlRepository.incrementClickCount(shortCode);

        } catch (Exception e) {
            // Never let analytics failure affect redirect
            log.error("Failed to record click for {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Returns full analytics for a short URL
     */
    public AnalyticsResponse getAnalytics(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new NoSuchElementException("Short URL not found"));

        // Build country map from query results
        Map<String, Long> byCountry = toMap(
                clickRepository.countByCountry(shortCode));

        // Build device map
        Map<String, Long> byDevice = toMap(
                clickRepository.countByDeviceType(shortCode));

        // Build referer map
        Map<String, Long> byReferer = toMap(
                clickRepository.countByReferer(shortCode));

        // Build time series
        List<Map<String, Object>> overTime = clickRepository
                .countByDay(shortCode)
                .stream()
                .map(row -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", row[0].toString());
                    point.put("clicks", row[1]);
                    return point;
                })
                .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .shortCode(shortCode)
                .originalUrl(url.getOriginalUrl())
                .totalClicks(url.getClickCount())
                .createdAt(url.getCreatedAt().format(DateTimeFormatter.ISO_DATE))
                .expiresAt(url.getExpiresAt() != null
                        ? url.getExpiresAt().format(DateTimeFormatter.ISO_DATE)
                        : "Never")
                .clicksByCountry(byCountry)
                .clicksByDevice(byDevice)
                .clicksByReferer(byReferer)
                .clicksOverTime(overTime)
                .build();
    }

    /**
     * Returns all URLs created by a user
     */
    public List<Url> getUserUrls(String userId) {
        return urlRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Converts list of Object[] query results to a clean Map
     * Each Object[] is [key, count] from the GROUP BY queries
     */
    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "Unknown";
            Long count = ((Number) row[1]).longValue();
            map.put(key, count);
        }
        return map;
    }
}