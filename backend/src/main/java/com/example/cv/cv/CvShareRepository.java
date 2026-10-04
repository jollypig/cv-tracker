package com.example.cv.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CvShareRepository extends JpaRepository<CvShare, UUID> {
    Optional<CvShare> findByCv_Id(UUID cvId);
    Optional<CvShare> findByTokenHash(String tokenHash);
    void deleteByCv_Id(UUID cvId);
}