package com.patient.demo.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, PatientAllergyId> {

    List<PatientAllergy> findAllByIdPatientIdAndIdAllergyIdIn(UUID patientId, Collection<UUID> allergyIds);

    Page<PatientAllergy> findAllByIdPatientId(UUID patientId, Pageable pageable);
}
