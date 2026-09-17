package com.patient.demo.allergy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AllergyUpdateRequest(
        @NotBlank @Size(max = 255) String name
) {

}