package com.patient.demo.common.utils;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class UUIDUtils {

    public static void checkDuplicates(List<UUID> ids) {
        Set<UUID> seen = new HashSet<>();
        Set<UUID> duplicates = ids.stream()
                .filter(id -> !seen.add(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!duplicates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate allergy ids in request: " + duplicates);
        }
    }
}
