package com.smarturl.util;

import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    /**
     * Detects device type from User-Agent string
     * Returns: "Mobile", "Tablet", or "Desktop"
     */
    public String getDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";

        String ua = userAgent.toLowerCase();

        // Check tablet first — tablets often contain "mobile" too
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        }

        if (ua.contains("mobile") || ua.contains("android") ||
                ua.contains("iphone") || ua.contains("ipod")) {
            return "Mobile";
        }

        return "Desktop";
    }

    /**
     * Detects browser from User-Agent string
     * Returns: "Chrome", "Firefox", "Safari", "Edge", or "Other"
     */
    public String getBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";

        String ua = userAgent.toLowerCase();

        // Order matters — Edge contains "chrome" so check Edge first
        if (ua.contains("edg/") || ua.contains("edge/")) return "Edge";
        if (ua.contains("chrome") && !ua.contains("chromium")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("opera") || ua.contains("opr/")) return "Opera";

        return "Other";
    }

    /**
     * Extracts clean referrer domain from full referrer URL
     * e.g. "https://twitter.com/some/path" → "twitter.com"
     */
    public String parseReferer(String referer) {
        if (referer == null || referer.isBlank()) return "Direct";

        try {
            // Remove protocol
            String cleaned = referer
                    .replace("https://", "")
                    .replace("http://", "");
            // Take just the domain part
            return cleaned.split("/")[0];
        } catch (Exception e) {
            return "Unknown";
        }
    }
}