package com.urlshortener.shortening.repository;

import com.urlshortener.shortening.domain.ShortLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);

    /** Atomically increments the denormalized click counter (avoids read-modify-write races). */
    @Modifying
    @Query("update ShortLink s set s.clickCount = s.clickCount + 1 where s.id = :id")
    int incrementClickCount(@Param("id") Long id);
}
