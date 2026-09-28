package com.patient.demo.allergy.controller;

import com.patient.demo.allergy.dto.AllergyCreateRequest;
import com.patient.demo.allergy.dto.AllergyResponse;
import com.patient.demo.allergy.dto.AllergyUpdateRequest;
import com.patient.demo.allergy.service.AllergyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/allergies")
@RequiredArgsConstructor
public class AllergyController {

    private final AllergyService service;

    @GetMapping
    public Page<AllergyResponse> findAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public AllergyResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public AllergyResponse save(@Valid @RequestBody AllergyCreateRequest createRequest) {
        return service.save(createRequest);
    }

    @PutMapping("/{id}")
    public AllergyResponse update(@PathVariable UUID id, @Valid @RequestBody AllergyUpdateRequest createRequest) {
        return service.update(id, createRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
