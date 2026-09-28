package com.patient.demo.address.dto;

import java.util.List;
import java.util.UUID;

public record AddressBatchResponse(

        List<AddressResponse> create,
        List<AddressResponse> update,
        List<UUID> delete
) {

}
