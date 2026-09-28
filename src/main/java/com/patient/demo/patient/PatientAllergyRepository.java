package com.patient.demo.patient;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, PatientAllergyId> {

}
