package com.patient.demo.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    Page<Address> findAllByPatientId(UUID patientId, Pageable pageable);

    List<Address> findAllByPatientId(UUID patientId);

    List<Address> findAllByIdInAndPatientId(Collection<UUID> ids, UUID patientId);
}
