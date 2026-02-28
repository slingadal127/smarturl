package com.smarturl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // Enables @Async annotation for non-blocking click recording
public class AsyncConfig {
    // Spring uses default thread pool for @Async methods
    // Good enough for our use case
}