package com.smarturl.repository;

import com.smarturl.model.Click;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {

    // Total click count for a short code
    long countByShortCode(String shortCode);

    // All clicks for a short code — used for time series chart
    List<Click> findByShortCodeOrderByClickedAtDesc(String shortCode);

    // Clicks within a time range — used for filtered analytics
    List<Click> findByShortCodeAndClickedAtBetween(
            String shortCode,
            LocalDateTime from,
            LocalDateTime to
    );

    // Group clicks by country — for geographic distribution chart
    // Returns list of [country, count] pairs
    @Query("SELECT c.country, COUNT(c) FROM Click c WHERE c.shortCode = :shortCode GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> countByCountry(String shortCode);

    // Group clicks by device type
    @Query("SELECT c.deviceType, COUNT(c) FROM Click c WHERE c.shortCode = :shortCode GROUP BY c.deviceType")
    List<Object[]> countByDeviceType(String shortCode);

    // Group clicks by referrer
    @Query("SELECT c.referer, COUNT(c) FROM Click c WHERE c.shortCode = :shortCode GROUP BY c.referer ORDER BY COUNT(c) DESC")
    List<Object[]> countByReferer(String shortCode);

    // Group clicks by day — for time series chart
    @Query("SELECT DATE(c.clickedAt), COUNT(c) FROM Click c WHERE c.shortCode = :shortCode GROUP BY DATE(c.clickedAt) ORDER BY DATE(c.clickedAt)")
    List<Object[]> countByDay(String shortCode);
}