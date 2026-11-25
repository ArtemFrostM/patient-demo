package com.patient.demo.patient.mapper;

import com.patient.demo.patient.Patient;
import com.patient.demo.patient.PatientAllergy;
import com.patient.demo.patient.dto.PatientAllergyCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyResponse;
import com.patient.demo.refdata.dto.RefDataAllergy;
import org.springframework.stereotype.Component;

@Component
public class PatientAllergyMapper {

    public PatientAllergy toEntity(Patient patient, PatientAllergyCreateRequest request) {
        PatientAllergy link = new PatientAllergy(patient, request.allergyId());
        link.setNotes(request.notes());
        link.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return link;
    }

    public PatientAllergyResponse toResponse(PatientAllergy link, RefDataAllergy refData) {
        return PatientAllergyResponse.patientAllergyResponse()
                .allergyId(link.getAllergyId())
                .name(refData == null ? null : refData.name())
                .sortOrder(link.getSortOrder())
                .notes(link.getNotes())
                .createdAt(link.getCreatedAt())
                .createdBy(link.getCreatedBy())
                .updatedAt(link.getUpdatedAt())
                .updatedBy(link.getUpdatedBy())
                .build();
    }
}
