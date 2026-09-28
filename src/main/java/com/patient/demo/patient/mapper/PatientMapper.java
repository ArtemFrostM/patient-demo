package com.patient.demo.patient.mapper;

import com.patient.demo.patient.Patient;
import com.patient.demo.patient.dto.PatientCreateRequest;
import com.patient.demo.patient.dto.PatientResponse;
import com.patient.demo.patient.dto.PatientUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public PatientResponse toResponse(Patient patient) {
        return PatientResponse.patientResponse()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .middleName(patient.getMiddleName())
                .dob(patient.getDob())
                .gender(patient.getGender())
                .createdAt(patient.getCreatedAt())
                .createdBy(patient.getCreatedBy())
                .updatedAt(patient.getUpdatedAt())
                .updatedBy(patient.getUpdatedBy())
                .build();
    }

    public Patient toEntity(PatientCreateRequest createRequest) {
        Patient patient = new Patient();
        patient.setFirstName(createRequest.firstName());
        patient.setLastName(createRequest.lastName());
        patient.setMiddleName(createRequest.middleName());
        patient.setDob(createRequest.dob());
        patient.setGender(createRequest.gender());
        return patient;
    }

    public void updateEntity(Patient patient, PatientUpdateRequest updateRequest) {
        patient.setFirstName(updateRequest.firstName());
        patient.setLastName(updateRequest.lastName());
        patient.setMiddleName(updateRequest.middleName());
        patient.setDob(updateRequest.dob());
        patient.setGender(updateRequest.gender());
    }
}
