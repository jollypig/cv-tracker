package com.example.cv.cv;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvRepository extends JpaRepository<Cv, UUID> {
    List<Cv> findAllByPersonIdOrderByUpdatedAtDesc(UUID personId);
    List<Cv> findAllByOrderByUpdatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cv from Cv cv where cv.id = :id")
    Optional<Cv> findByIdForUpdate(@Param("id") UUID id);
}