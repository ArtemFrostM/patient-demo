package com.patient.demo.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Page<Patient> findAllByIsDeletedFalse(Pageable pageable);

    Optional<Patient> findByIdAndIsDeletedFalse(UUID id);

}
