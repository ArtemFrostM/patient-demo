package com.patient.demo.patient.service;

import com.patient.demo.patient.PatientAllergy;
import com.patient.demo.patient.PatientAllergyRepository;
import com.patient.demo.patient.PatientRepository;
import com.patient.demo.patient.dto.PatientAllergyCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyPatchRequest;
import com.patient.demo.patient.dto.PatientAllergyResponse;
import com.patient.demo.patient.dto.PatientAllergyUpdateRequest;
import com.patient.demo.patient.mapper.PatientAllergyMapper;
import com.patient.demo.refdata.client.RefDataClient;
import com.patient.demo.refdata.dto.RefDataAllergy;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Stream;

import static com.patient.demo.common.utils.UUIDUtils.checkDuplicates;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
public class PatientAllergyService {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("sortOrder", "createdAt");

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final RefDataClient refDataClient;
    private final PatientAllergyWriteService writeService;
    private final PatientAllergyMapper mapper;

    public Page<PatientAllergyResponse> findAll(UUID patientId, Pageable pageable) {
        rejectUnsortable(pageable.getSort());
        requirePatient(patientId);
        Page<PatientAllergy> links = patientAllergyRepository.findAllByIdPatientId(patientId, pageable);
        Map<UUID, RefDataAllergy> refData = fetchFromRefData(links.map(PatientAllergy::getAllergyId).getContent());
        return links.map(link -> mapper.toResponse(link, refData.get(link.getAllergyId())));
    }

    public void patch(UUID patientId, PatientAllergyPatchRequest request) {
        List<UUID> createIds = getCreateIds(request);
        List<UUID> updateIds = getUpdateIds(request);
        List<UUID> deleteIds = request.delete();

        checkDuplicates(getAllUuids(createIds, updateIds, deleteIds));

        requirePatient(patientId);
        checkAlreadyLinked(patientId, createIds);
        checkNotLinked(patientId, concat(updateIds, deleteIds));

        if (!createIds.isEmpty()) {
            checkUnknown(createIds, fetchFromRefData(createIds).keySet());
        }

        try {
            writeService.apply(patientId, request);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Allergy already linked (concurrent request)", e);
        }
    }

    private List<UUID> getAllUuids(List<UUID> createIds, List<UUID> updateIds, List<UUID> deleteIds) {
        return Stream.of(createIds, updateIds, deleteIds)
                .flatMap(List::stream)
                .toList();
    }

    private List<UUID> concat(List<UUID> updateIds, List<UUID> deleteIds) {
        return Stream.of(updateIds, deleteIds)
                .flatMap(List::stream)
                .toList();
    }

    private List<UUID> getCreateIds(PatientAllergyPatchRequest request) {
        return request.create()
                .stream()
                .map(PatientAllergyCreateRequest::allergyId)
                .toList();
    }

    private List<UUID> getUpdateIds(PatientAllergyPatchRequest request) {
        return request.update()
                .stream()
                .map(PatientAllergyUpdateRequest::allergyId)
                .toList();
    }

    private void checkNotLinked(UUID patientId, List<UUID> ids) {
        if (ids.isEmpty()) return;
        Set<UUID> linked = patientAllergyRepository.findAllByIdPatientIdAndIdAllergyIdIn(patientId, ids).stream()
                .map(PatientAllergy::getAllergyId).collect(toSet());
        List<UUID> missing = ids.stream().filter(id -> !linked.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Allergies not linked to patient: " + missing);
        }
    }

    private void requirePatient(UUID patientId) {
        patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with id: " + patientId));
    }

    private Map<UUID, RefDataAllergy> fetchFromRefData(Collection<UUID> allergyIds) {
        return refDataClient.findAllergiesByIds(new LinkedHashSet<>(allergyIds)).stream()
                .collect(toMap(RefDataAllergy::id, identity()));
    }

    private void checkAlreadyLinked(UUID patientId, List<UUID> ids) {
        List<UUID> linked = patientAllergyRepository.findAllByIdPatientIdAndIdAllergyIdIn(patientId, ids).stream()
                .map(PatientAllergy::getAllergyId)
                .toList();
        if (!linked.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Allergies already linked to patient: " + linked);
        }
    }

    private void checkUnknown(List<UUID> requested, Set<UUID> found) {
        Set<UUID> unknown = new LinkedHashSet<>(requested);
        unknown.removeAll(found);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown allergy ids: " + unknown);
        }
    }

    private static void rejectUnsortable(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_PROPERTIES);
            }
        }
    }
}
