package com.patient.demo.patient.service;

import com.patient.demo.patient.Patient;
import com.patient.demo.patient.PatientAllergy;
import com.patient.demo.patient.PatientAllergyId;
import com.patient.demo.patient.PatientAllergyRepository;
import com.patient.demo.patient.PatientRepository;
import com.patient.demo.patient.dto.PatientAllergiesCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyResponse;
import com.patient.demo.patient.mapper.PatientAllergyMapper;
import com.patient.demo.refdata.client.RefDataClient;
import com.patient.demo.refdata.dto.RefDataAllergy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PatientAllergyService {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final RefDataClient refDataClient;
    private final PatientAllergyMapper mapper;

    public List<PatientAllergyResponse> findAll(UUID patientId) {
        requirePatient(patientId);
        List<PatientAllergy> links = patientAllergyRepository.findAllByIdPatientIdOrderBySortOrderAsc(patientId);
        Map<UUID, RefDataAllergy> refData = fetchFromRefData(links.stream().map(PatientAllergy::getAllergyId).toList());
        return links.stream()
                .map(link -> mapper.toResponse(link, refData.get(link.getAllergyId())))
                .toList();
    }

    @Transactional
    public List<PatientAllergyResponse> addAll(UUID patientId, PatientAllergiesCreateRequest request) {
        Patient patient = requirePatient(patientId);

        List<UUID> requestedIds = request.allergies().stream().map(PatientAllergyCreateRequest::allergyId).toList();
        // cheap local checks first: no point calling ref-data for a request that is rejected anyway
        rejectDuplicates(requestedIds);
        rejectAlreadyLinked(patientId, requestedIds);

        Map<UUID, RefDataAllergy> refData = fetchFromRefData(requestedIds);
        rejectUnknown(requestedIds, refData.keySet());

        List<PatientAllergy> links = request.allergies().stream()
                .map(allergy -> mapper.toEntity(patient, allergy))
                .toList();
        return patientAllergyRepository.saveAll(links).stream()
                .map(link -> mapper.toResponse(link, refData.get(link.getAllergyId())))
                .toList();
    }

    @Transactional
    public void delete(UUID patientId, UUID allergyId) {
        requirePatient(patientId);
        PatientAllergyId id = new PatientAllergyId(patientId, allergyId);
        if (!patientAllergyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Allergy " + allergyId + " is not linked to patient " + patientId);
        }
        patientAllergyRepository.deleteById(id);
    }

    private Patient requirePatient(UUID patientId) {
        return patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with id: " + patientId));
    }

    private Map<UUID, RefDataAllergy> fetchFromRefData(Collection<UUID> allergyIds) {
        return refDataClient.findAllergiesByIds(new LinkedHashSet<>(allergyIds)).stream()
                .collect(toMap(RefDataAllergy::id, identity()));
    }

    private static void rejectDuplicates(List<UUID> ids) {
        Set<UUID> seen = new HashSet<>();
        Set<UUID> duplicates = ids.stream()
                .filter(id -> !seen.add(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!duplicates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate allergy ids in request: " + duplicates);
        }
    }

    private void rejectAlreadyLinked(UUID patientId, List<UUID> ids) {
        List<UUID> linked = patientAllergyRepository.findAllByIdPatientIdAndIdAllergyIdIn(patientId, ids).stream()
                .map(PatientAllergy::getAllergyId)
                .toList();
        if (!linked.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Allergies already linked to patient: " + linked);
        }
    }

    private static void rejectUnknown(List<UUID> requested, Set<UUID> found) {
        Set<UUID> unknown = new LinkedHashSet<>(requested);
        unknown.removeAll(found);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown allergy ids: " + unknown);
        }
    }
}
