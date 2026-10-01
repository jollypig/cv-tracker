package com.example.cv.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CvRepository extends JpaRepository<Cv, UUID> {
    List<Cv> findAllByPersonIdOrderByUpdatedAtDesc(UUID personId);
    List<Cv> findAllByOrderByUpdatedAtDesc();
}