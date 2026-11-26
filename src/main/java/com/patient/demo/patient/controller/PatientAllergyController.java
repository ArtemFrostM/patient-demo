package com.patient.demo.patient.controller;

import com.patient.demo.patient.dto.PatientAllergyPatchRequest;
import com.patient.demo.patient.dto.PatientAllergyResponse;
import com.patient.demo.patient.service.PatientAllergyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/allergies")
@RequiredArgsConstructor
public class PatientAllergyController {

    private final PatientAllergyService service;

    // Left for the memories
//    @GetMapping
//    public List<PatientAllergyResponse> findAll(@PathVariable UUID patientId) {
//        return service.findAll(patientId);
//    }

    @GetMapping
    public PagedModel<PatientAllergyResponse> findAll(
            @PathVariable UUID patientId,
            @PageableDefault(size = 20, sort = "sortOrder") Pageable pageable) {
        return new PagedModel<>(service.findAll(patientId, pageable));
    }

    @PatchMapping
    @ResponseStatus(NO_CONTENT)
    public void patch(@PathVariable UUID patientId, @Valid @RequestBody PatientAllergyPatchRequest request) {
        service.patch(patientId, request);
    }

    // Left for the memories
//    @PostMapping
//    @ResponseStatus(CREATED)
//    public List<PatientAllergyResponse> addAll(@PathVariable UUID patientId,
//                                               @Valid @RequestBody PatientAllergiesCreateRequest request) {
//        return service.addAll(patientId, request);
//    }
//
//    @DeleteMapping("/{allergyId}")
//    @ResponseStatus(NO_CONTENT)
//    public void delete(@PathVariable UUID patientId, @PathVariable UUID allergyId) {
//        service.delete(patientId, allergyId);
//    }
}
