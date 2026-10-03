package com.example.cv.person;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
	List<Person> findAllByOwner_IdOrderByLastNameAscFirstNameAscIdAsc(UUID ownerId);
	Optional<Person> findByIdAndOwner_Id(UUID id, UUID ownerId);
	boolean existsByIdAndOwner_Id(UUID id, UUID ownerId);
}