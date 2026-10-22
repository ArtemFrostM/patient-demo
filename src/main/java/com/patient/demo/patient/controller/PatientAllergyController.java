package com.patient.demo.patient.controller;

import com.patient.demo.patient.dto.PatientAllergiesCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyResponse;
import com.patient.demo.patient.service.PatientAllergyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/allergies")
@RequiredArgsConstructor
public class PatientAllergyController {

    private final PatientAllergyService service;

    @GetMapping
    public List<PatientAllergyResponse> findAll(@PathVariable UUID patientId) {
        return service.findAll(patientId);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public List<PatientAllergyResponse> addAll(@PathVariable UUID patientId,
                                               @Valid @RequestBody PatientAllergiesCreateRequest request) {
        return service.addAll(patientId, request);
    }

    @DeleteMapping("/{allergyId}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID patientId, @PathVariable UUID allergyId) {
        service.delete(patientId, allergyId);
    }
}
