package com.patient.demo.patient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

public record PatientAllergyPatchRequest(
        @Size(max = 100) List<@Valid @NotNull PatientAllergyCreateRequest> create,
        @Size(max = 100) List<@Valid @NotNull PatientAllergyUpdateRequest> update,
        @Size(max = 100) List<@NotNull UUID> delete) {

    public PatientAllergyPatchRequest {
        create = isNull(create) ? new ArrayList<>() : create;
        update = isNull(update) ? new ArrayList<>() : update;
        delete = isNull(delete) ? new ArrayList<>() : delete;
    }

    @AssertTrue()
    boolean isNotEmpty() {
        return !create.isEmpty() || !update.isEmpty() || !delete.isEmpty();
    }
}
