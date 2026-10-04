package com.example.cv.cv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CvShareRepository extends JpaRepository<CvShare, UUID> {
    Optional<CvShare> findByCv_Id(UUID cvId);
    Optional<CvShare> findByTokenHash(String tokenHash);
    void deleteByCv_Id(UUID cvId);

    @Modifying
    @Query(value = "UPDATE cv_share SET view_count = view_count + 1, last_viewed_at = :viewedAt WHERE id = :shareId", nativeQuery = true)
    void recordView(@Param("shareId") UUID shareId, @Param("viewedAt") Instant viewedAt);
}