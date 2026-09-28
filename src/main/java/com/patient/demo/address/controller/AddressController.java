package com.patient.demo.address.controller;

import com.patient.demo.address.AddressService;
import com.patient.demo.address.dto.AddressBatchRequest;
import com.patient.demo.address.dto.AddressBatchResponse;
import com.patient.demo.address.dto.AddressResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public Page<AddressResponse> findAll(@PathVariable UUID patientId,
                                         @PageableDefault(size = 20, sort = {"sortOrder", "id"}) Pageable pageable) {
        return addressService.findAll(patientId, pageable);
    }

    @PatchMapping
    public AddressBatchResponse applyBatch(@PathVariable UUID patientId, @Valid @RequestBody AddressBatchRequest batchRequest) {
        return addressService.applyBatch(patientId, batchRequest);
    }
}
