package com.patient.demo.patient.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PatientAllergyCreateRequest(

        @NotNull UUID allergyId,
        @Size(max = 1000) String notes,
        @PositiveOrZero Integer sortOrder
) {

}
