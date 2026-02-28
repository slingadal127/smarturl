package com.smarturl.repository;

import com.smarturl.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    // Spring generates SQL automatically from method name:
    // SELECT * FROM urls WHERE short_code = ?
    Optional<Url> findByShortCode(String shortCode);

    // SELECT * FROM urls WHERE user_id = ? ORDER BY created_at DESC
    List<Url> findByUserIdOrderByCreatedAtDesc(String userId);

    // Check if short code already exists
    boolean existsByShortCode(String shortCode);

    // Increment click count atomically
    // @Modifying tells Spring this query modifies data
    @Modifying
    @Transactional
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(String shortCode);
}