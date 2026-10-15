package com.patient.demo.address;

import com.patient.demo.address.dto.AddressCreateRequest;
import com.patient.demo.address.dto.AddressResponse;
import com.patient.demo.address.dto.AddressUpdateRequest;
import com.patient.demo.patient.Patient;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponse toResponse(Address address) {
        return AddressResponse.addressResponse()
                .id(address.getId())
                // getId() on the lazy proxy is served from the foreign key, no extra select
                .patientId(address.getPatient().getId())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .sortOrder(address.getSortOrder())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .zipCode(address.getZipCode())
                .city(address.getCity())
                .country(address.getCountry())
                .createdAt(address.getCreatedAt())
                .createdBy(address.getCreatedBy())
                .updatedAt(address.getUpdatedAt())
                .updatedBy(address.getUpdatedBy())
                .build();
    }


    public Address toEntity(AddressCreateRequest createRequest, Patient patient) {
        Address address = new Address();
        address.setPatient(patient);
        address.setAddressType(createRequest.addressType());
        address.setDefault(createRequest.isDefault());
        address.setSortOrder(createRequest.sortOrder());
        address.setLine1(createRequest.line1());
        address.setLine2(createRequest.line2());
        address.setZipCode(createRequest.zipCode());
        address.setCity(createRequest.city());
        address.setCountry(createRequest.country());
        return address;
    }


    public void updateEntity(Address address, AddressUpdateRequest updateRequest) {
        address.setAddressType(updateRequest.addressType());
        address.setDefault(updateRequest.isDefault());
        address.setSortOrder(updateRequest.sortOrder());
        address.setLine1(updateRequest.line1());
        address.setLine2(updateRequest.line2());
        address.setZipCode(updateRequest.zipCode());
        address.setCity(updateRequest.city());
        address.setCountry(updateRequest.country());
    }
}
