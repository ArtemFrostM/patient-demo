package com.patient.demo.allergy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AllergyCreateRequest(
        @NotBlank @Size(max = 255) String name
) {

}
