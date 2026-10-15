package com.patient.demo.address.dto;

import com.patient.demo.address.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AddressCreateRequest(

        @NotNull AddressType addressType,
        @NotNull Boolean isDefault,
        @NotNull @PositiveOrZero Integer sortOrder,
        @NotBlank @Size(max = 255) String line1,
        @Size(max = 255) String line2,
        @NotBlank @Size(max = 20) String zipCode,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String country
) {

}
