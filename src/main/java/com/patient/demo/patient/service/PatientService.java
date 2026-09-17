package com.patient.demo.patient.service;

import com.patient.demo.patient.Patient;
import com.patient.demo.patient.PatientRepository;
import com.patient.demo.patient.dto.PatientCreateRequest;
import com.patient.demo.patient.dto.PatientResponse;
import com.patient.demo.patient.dto.PatientUpdateRequest;
import com.patient.demo.patient.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public Page<PatientResponse> findAll(Pageable pageable) {
        return patientRepository.findAllByIsDeletedFalse(pageable).map(patientMapper::toResponse);
    }

    public PatientResponse findById(UUID id) {
        return patientRepository.findByIdAndIsDeletedFalse(id)
                .map(patientMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with id: " + id));
    }

    @Transactional
    public PatientResponse save(PatientCreateRequest createRequest) {
        Patient patient = patientMapper.toEntity(createRequest);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    //TODO doublecheck this logic
    @Transactional
    public PatientResponse update(UUID id, PatientUpdateRequest updateRequest) {
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with id: " + id));
        patientMapper.updateEntity(patient, updateRequest);
        return patientMapper.toResponse(patient);
    }

    @Transactional
    public void delete(UUID id) {
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with id: " + id));
        patient.setDeleted(true);
    }
}
