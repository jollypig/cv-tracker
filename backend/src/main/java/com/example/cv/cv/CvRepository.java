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
    List<Cv> findAllByPerson_IdAndPerson_Owner_IdOrderByUpdatedAtDesc(UUID personId, UUID ownerId);
    List<Cv> findAllByPerson_Owner_IdOrderByUpdatedAtDesc(UUID ownerId);
    List<Cv> findAllByOrderByUpdatedAtDesc();
    boolean existsByIdAndPerson_Owner_Id(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cv from Cv cv where cv.id = :id")
    Optional<Cv> findByIdForUpdate(@Param("id") UUID id);
}