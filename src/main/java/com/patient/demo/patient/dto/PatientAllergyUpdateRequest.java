package com.patient.demo.patient.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PatientAllergyUpdateRequest(
        @NotNull UUID allergyId,
        @Size(max = 1000) String notes,
        @PositiveOrZero Integer sortOrder
) {

    @AssertTrue(message = "update item must change at least one field")
    boolean isChangingSomething() {
        return notes != null || sortOrder != null;
    }
}
