package com.patient.demo.patient;

import com.patient.demo.patient.dto.PatientAllergiesCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyResponse;
import com.patient.demo.patient.mapper.PatientAllergyMapper;
import com.patient.demo.patient.service.PatientAllergyService;
import com.patient.demo.refdata.client.RefDataClient;
import com.patient.demo.refdata.dto.RefDataAllergy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for {@link PatientAllergyService}: repositories and ref-data client mocked, mapper real.
 *
 * <p>The focus is the cross-service contract: ids must exist in ref-data-service on create, and reads
 * are enriched with names from ref-data-service.
 */
@ExtendWith(MockitoExtension.class)
class PatientAllergyServiceTest {

    private static final UUID PATIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PEANUTS = UUID.fromString("8f1c0a6e-0b1a-4c1e-9a01-000000000001");
    private static final UUID MILK = UUID.fromString("8f1c0a6e-0b1a-4c1e-9a01-000000000003");
    private static final UUID UNKNOWN = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientAllergyRepository patientAllergyRepository;

    @Mock
    private RefDataClient refDataClient;

    private PatientAllergyService service;
    private Patient patient;

    @BeforeEach
    void setUp() {
        service = new PatientAllergyService(patientRepository, patientAllergyRepository, refDataClient, new PatientAllergyMapper());
        patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDob(LocalDate.of(1990, 5, 17));
        patient.setGender(Gender.MALE);
    }

    private static PatientAllergiesCreateRequest request(UUID... ids) {
        return new PatientAllergiesCreateRequest(Arrays.stream(ids)
                .map(id -> new PatientAllergyCreateRequest(id, null, 0))
                .toList());
    }

    @Test
    @DisplayName("addAll: unknown id -> 400 naming only the unknown id, nothing saved")
    void rejectsUnknownIds() {
        when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(refDataClient.findAllergiesByIds(anyCollection())).thenReturn(List.of(new RefDataAllergy(PEANUTS, "Peanuts")));

        assertThatThrownBy(() -> service.addAll(PATIENT_ID, request(PEANUTS, UNKNOWN)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).contains(UNKNOWN.toString()).doesNotContain(PEANUTS.toString());
                });
        verify(patientAllergyRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("addAll: duplicate ids in request -> 400 before calling ref-data")
    void rejectsDuplicatesWithoutRemoteCall() {
        when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.addAll(PATIENT_ID, request(PEANUTS, PEANUTS)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(refDataClient);
    }

    @Test
    @DisplayName("addAll: already linked -> 409 before calling ref-data")
    void rejectsAlreadyLinkedWithoutRemoteCall() {
        when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(patientAllergyRepository.findAllByIdPatientIdAndIdAllergyIdIn(eq(PATIENT_ID), anyCollection()))
                .thenReturn(List.of(new PatientAllergy(patient, PEANUTS)));

        assertThatThrownBy(() -> service.addAll(PATIENT_ID, request(PEANUTS)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(refDataClient);
    }

    @Test
    @DisplayName("addAll: patient not found -> 404 without calling ref-data")
    void patientNotFound() {
        when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addAll(PATIENT_ID, request(PEANUTS)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(refDataClient);
    }

    @Test
    @DisplayName("addAll: valid ids -> saved and returned with names from ref-data")
    void savesAndEnriches() {
        when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(refDataClient.findAllergiesByIds(anyCollection())).thenReturn(List.of(new RefDataAllergy(PEANUTS, "Peanuts")));
        when(patientAllergyRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PatientAllergyResponse> result = service.addAll(PATIENT_ID, request(PEANUTS));

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.allergyId()).isEqualTo(PEANUTS);
            assertThat(response.name()).isEqualTo("Peanuts");
        });
    }

    @Test
    @DisplayName("findAll: enriches names; allergy missing in ref-data -> name null")
    void findAllEnriches() {
        when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(patientAllergyRepository.findAllByIdPatientIdOrderBySortOrderAsc(PATIENT_ID))
                .thenReturn(List.of(new PatientAllergy(patient, PEANUTS), new PatientAllergy(patient, MILK)));
        when(refDataClient.findAllergiesByIds(anyCollection())).thenReturn(List.of(new RefDataAllergy(PEANUTS, "Peanuts")));

        List<PatientAllergyResponse> result = service.findAll(PATIENT_ID);

        assertThat(result)
                .extracting(PatientAllergyResponse::allergyId, PatientAllergyResponse::name)
                .containsExactly(tuple(PEANUTS, "Peanuts"), tuple(MILK, null));
    }

    @Test
    @DisplayName("ref-data unavailable -> 503 propagates, nothing saved")
    void refDataUnavailable() {
        when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(refDataClient.findAllergiesByIds(anyCollection()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ref-data-service is unavailable"));

        assertThatThrownBy(() -> service.addAll(PATIENT_ID, request(PEANUTS)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(patientAllergyRepository, never()).saveAll(anyList());
    }
}
