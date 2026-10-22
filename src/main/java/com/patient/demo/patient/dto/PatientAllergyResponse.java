package com.patient.demo.patient.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(builderMethodName = "patientAllergyResponse")
public record PatientAllergyResponse(
        UUID allergyId,
        String name,
        int sortOrder,
        String notes,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {

}
