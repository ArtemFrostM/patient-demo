package com.patient.demo.address;

import com.patient.demo.address.dto.AddressBatchRequest;
import com.patient.demo.address.dto.AddressBatchResponse;
import com.patient.demo.address.dto.AddressResponse;
import com.patient.demo.address.dto.AddressUpdateRequest;
import com.patient.demo.patient.Patient;
import com.patient.demo.patient.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
public class AddressService {

    private final AddressRepository addressRepository;
    private final PatientRepository patientRepository;
    private final AddressMapper addressMapper;

    public Page<AddressResponse> findAll(UUID patientId, Pageable pageable) {
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with id: " + patientId));
        return addressRepository.findAllByPatientId(patientId, pageable).map(addressMapper::toResponse);
    }

    @Transactional
    public AddressBatchResponse applyBatch(UUID patientId, AddressBatchRequest batchRequest) {
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with id: " + patientId));

        List<UUID> updateIds = collectUpdateIds(batchRequest.update());
        Set<UUID> deleteIds = distinctDeleteIds(batchRequest.delete());
        rejectOverlap(updateIds, deleteIds);

        List<Address> addressesToDelete = addressRepository.findAllByIdInAndPatientId(deleteIds, patientId);
        if (addressesToDelete.size() != deleteIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some addresses not found for deletion");
        }
        addressRepository.deleteAll(addressesToDelete);

        addressRepository.flush();

        List<Address> existing = addressRepository.findAllByIdInAndPatientId(updateIds, patientId);
        if (existing.size() != updateIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "...");
        }
        Map<UUID, AddressUpdateRequest> byId = batchRequest.update().stream()
                .collect(toMap(AddressUpdateRequest::id, identity()));
        existing.forEach(address -> addressMapper.updateEntity(address, byId.get(address.getId())));

        List<Address> created = batchRequest.create().stream()
                .map(request -> addressMapper.toEntity(request, patient))
                .toList();
        List<Address> createdSaved = addressRepository.saveAll(created);

        addressRepository.flush();

        long defaults = getDefaultCount(patientId);
        if (defaults > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "To many default addresses");
        }

        return new AddressBatchResponse(
                createdSaved.stream().map(addressMapper::toResponse).toList(),
                existing.stream().map(addressMapper::toResponse).toList(),
                new ArrayList<>(deleteIds)
        );
    }

    private long getDefaultCount(UUID patientId) {
        return addressRepository.findAllByPatientId(patientId).stream()
                .filter(Address::isDefault).count();
    }

    private List<UUID> collectUpdateIds(List<AddressUpdateRequest> updateRequests) {
        List<UUID> ids = updateRequests.stream()
                .map(AddressUpdateRequest::id)
                .toList();
        Set<UUID> duplicates = duplicatesOf(ids);
        if (!duplicates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate address ids in 'update': " + duplicates);
        }
        return ids;
    }

    private static Set<UUID> distinctDeleteIds(List<UUID> deleteRequests) {
        return new LinkedHashSet<>(deleteRequests);
    }

    private static void rejectOverlap(Collection<UUID> updateIds, Collection<UUID> deleteIds) {
        Set<UUID> overlap = new LinkedHashSet<>(updateIds);
        overlap.retainAll(deleteIds);
        if (!overlap.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address ids present in both 'update' and 'delete': " + overlap);
        }
    }

    private static Set<UUID> duplicatesOf(Collection<UUID> ids) {
        Set<UUID> seen = new HashSet<>();
        return ids.stream()
                .filter(id -> !seen.add(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
