package com.patient.demo.patient.service;

import com.patient.demo.patient.Patient;
import com.patient.demo.patient.PatientAllergy;
import com.patient.demo.patient.PatientAllergyRepository;
import com.patient.demo.patient.PatientRepository;
import com.patient.demo.patient.dto.PatientAllergyCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyPatchRequest;
import com.patient.demo.patient.dto.PatientAllergyUpdateRequest;
import com.patient.demo.patient.mapper.PatientAllergyMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PatientAllergyWriteService {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientAllergyMapper mapper;

    @Transactional
    public void apply(UUID patientId, PatientAllergyPatchRequest request) {
        deleteLinks(patientId, request.delete());
        updateLinks(patientId, request.update());
        createLinks(patientId, request.create());
    }

    private void deleteLinks(UUID patientId, List<UUID> allergyIds) {
        if (allergyIds.isEmpty()) return;
        List<PatientAllergy> links = loadExisting(patientId, allergyIds);
        patientAllergyRepository.deleteAllInBatch(links);
    }

    private void updateLinks(UUID patientId, List<PatientAllergyUpdateRequest> items) {
        if (items.isEmpty()) return;
        Map<UUID, PatientAllergy> links = loadExisting(patientId, getUuids(items)).stream()
                .collect(toMap(PatientAllergy::getAllergyId, identity()));
        items.forEach(item -> mapper.applyUpdate(links.get(item.allergyId()), item));
    }

    private List<UUID> getUuids(List<PatientAllergyUpdateRequest> items) {
        return items.stream()
                .map(PatientAllergyUpdateRequest::allergyId)
                .toList();
    }

    private void createLinks(UUID patientId, List<PatientAllergyCreateRequest> items) {
        if (items.isEmpty()) return;
        Patient patient = patientRepository.getReferenceById(patientId);
        patientAllergyRepository.saveAll(items.stream().map(i -> mapper.toEntity(patient, i)).toList());
    }

    private List<PatientAllergy> loadExisting(UUID patientId, List<UUID> allergyIds) {
        List<PatientAllergy> links = patientAllergyRepository.findAllByIdPatientIdAndIdAllergyIdIn(patientId, allergyIds);
        if (links.size() != allergyIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Allergy links changed concurrently, retry");
        }
        return links;
    }
}
