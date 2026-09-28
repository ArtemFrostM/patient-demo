package com.patient.demo.patient.dto;

import com.patient.demo.patient.Gender;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder(builderMethodName = "patientResponse")
public record PatientResponse(
        UUID id,
        String firstName,
        String lastName,
        String middleName,
        LocalDate dob,
        Gender gender,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {

}
