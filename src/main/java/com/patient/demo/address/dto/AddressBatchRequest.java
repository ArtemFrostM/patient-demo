package com.patient.demo.address.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

public record AddressBatchRequest(

        @Valid @Size(max = 500) List<@NotNull @Valid AddressCreateRequest> create,
        @Valid @Size(max = 500) List<@NotNull @Valid AddressUpdateRequest> update,
        @Size(max = 500) List<@NotNull UUID> delete
) {

    public AddressBatchRequest {
        create = nullSafe(create);
        update = nullSafe(update);
        delete = nullSafe(delete);
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return isNull(list) ? List.of() : list;
    }
}
