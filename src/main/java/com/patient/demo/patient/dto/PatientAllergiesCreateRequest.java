package com.patient.demo.patient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PatientAllergiesCreateRequest(

        @NotEmpty @Size(max = 50) List<@NotNull @Valid PatientAllergyCreateRequest> allergies
) {

}
