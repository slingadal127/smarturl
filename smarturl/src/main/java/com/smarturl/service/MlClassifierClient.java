package com.smarturl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class MlClassifierClient {

    private final WebClient webClient;
    private static final String ML_URL = "http://localhost:8083";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    public MlClassifierClient() {
        this.webClient = WebClient.builder()
                .baseUrl(ML_URL)
                .build();
    }

    /**
     * Calls ML service to classify a URL as safe or malicious
     * Returns null if ML service is unavailable — caller handles fallback
     */
    public Map<String, Object> classify(String url) {
        try {
            Map response = webClient.post()
                    .uri("/ml/classify")
                    .bodyValue(Map.of("url", url))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();
            return response;
        } catch (Exception e) {
            log.warn("ML service unavailable: {}. Allowing URL.", e.getMessage());
            return null;
        }
    }

    public boolean isHealthy() {
        try {
            Map response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(2))
                    .block();
            return response != null;
        } catch (Exception e) {
            return false;
        }
    }
}