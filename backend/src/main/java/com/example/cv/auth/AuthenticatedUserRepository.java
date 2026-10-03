package com.example.cv.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthenticatedUserRepository extends JpaRepository<AuthenticatedUser, UUID> {
    Optional<AuthenticatedUser> findByIssuerAndSubject(String issuer, String subject);
}