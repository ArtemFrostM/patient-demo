package com.patient.demo.address.dto;

import com.patient.demo.address.AddressType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(builderMethodName = "addressResponse")
public record AddressResponse (

        UUID id,
        UUID patientId,
        AddressType addressType,
        boolean isDefault,
        int sortOrder,
        String line1,
        String line2,
        String zipCode,
        String city,
        String country,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {

}
