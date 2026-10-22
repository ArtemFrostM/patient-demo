package com.patient.demo.refdata.client;

import com.patient.demo.refdata.dto.RefDataAllergy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefDataClient {

    private static final ParameterizedTypeReference<List<RefDataAllergy>> ALLERGY_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient refDataRestClient;

    public List<RefDataAllergy> findAllergiesByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        try {
            List<RefDataAllergy> allergies = refDataRestClient.get()
                    .uri(uri -> uri.path("/api/v1/allergies/lookup").queryParam("ids", ids).build())
                    .retrieve()
                    .body(ALLERGY_LIST);
            return allergies == null ? List.of() : allergies;
        } catch (RestClientException | OAuth2AuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ref-data-service is unavailable", e);
        }
    }
}
