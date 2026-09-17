package com.patient.demo.allergy.mapper;

import com.patient.demo.allergy.Allergy;
import com.patient.demo.allergy.dto.AllergyCreateRequest;
import com.patient.demo.allergy.dto.AllergyResponse;
import com.patient.demo.allergy.dto.AllergyUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class AllergyMapper {

    public AllergyResponse toResponse(Allergy allergy) {
        return AllergyResponse.allergyResponse()
                .id(allergy.getId())
                .name(allergy.getName())
                .createdAt(allergy.getCreatedAt())
                .createdBy(allergy.getCreatedBy())
                .updatedAt(allergy.getUpdatedAt())
                .updatedBy(allergy.getUpdatedBy())
                .build();
    }

    public Allergy toEntity(AllergyCreateRequest request) {
        Allergy allergy = new Allergy();
        allergy.setName(request.name());
        return allergy;
    }

    public Allergy updateEntity(Allergy entity, AllergyUpdateRequest request) {
        entity.setName(request.name());
        return entity;
    }
}
