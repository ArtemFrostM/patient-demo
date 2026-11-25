package com.patient.demo.patient;

import com.patient.demo.patient.dto.PatientAllergyCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyPatchRequest;
import com.patient.demo.patient.dto.PatientAllergyResponse;
import com.patient.demo.patient.dto.PatientAllergyUpdateRequest;
import com.patient.demo.patient.mapper.PatientAllergyMapper;
import com.patient.demo.patient.service.PatientAllergyService;
import com.patient.demo.patient.service.PatientAllergyWriteService;
import com.patient.demo.refdata.client.RefDataClient;
import com.patient.demo.refdata.dto.RefDataAllergy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for {@link PatientAllergyService}: repositories, ref-data client and the transactional
 * writer are mocked, the mapper is real.
 *
 * <p>The focus is the orchestration order of PATCH: cheap checks first, the ref-data call only when it is
 * needed, and the writer (the only place with a transaction) only after every check passed.
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

    @Mock
    private PatientAllergyWriteService writeService;

    private PatientAllergyService service;
    private Patient patient;

    @BeforeEach
    void setUp() {
        service = new PatientAllergyService(patientRepository, patientAllergyRepository, refDataClient,
                writeService, new PatientAllergyMapper());
        patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDob(LocalDate.of(1990, 5, 17));
        patient.setGender(Gender.MALE);
    }

    // ---------------------------------------------------------------- helpers

    private void patientExists() {
        when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.of(patient));
    }

    /**
     * Simulates the DB for both "already linked" and "not linked" checks: whatever ids the service asks
     * about, only those that are in {@code linkedIds} come back.
     */
    private void alreadyLinked(UUID... linkedIds) {
        List<UUID> linked = List.of(linkedIds);
        when(patientAllergyRepository.findAllByIdPatientIdAndIdAllergyIdIn(eq(PATIENT_ID), anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<UUID> asked = invocation.getArgument(1);
                    return asked.stream().filter(linked::contains).map(id -> new PatientAllergy(patient, id)).toList();
                });
    }

    private void refDataKnows(RefDataAllergy... allergies) {
        when(refDataClient.findAllergiesByIds(anyCollection())).thenReturn(List.of(allergies));
    }

    private static PatientAllergyPatchRequest create(UUID... ids) {
        return new PatientAllergyPatchRequest(
                Arrays.stream(ids).map(id -> new PatientAllergyCreateRequest(id, null, 0)).toList(), null, null);
    }

    private static PatientAllergyCreateRequest createItem(UUID id) {
        return new PatientAllergyCreateRequest(id, null, null);
    }

    private static PatientAllergyUpdateRequest updateItem(UUID id) {
        return new PatientAllergyUpdateRequest(id, "updated", null);
    }

    private static void assertStatus(Throwable thrown, HttpStatus expected) {
        assertThat(thrown).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(expected));
    }

    // ---------------------------------------------------------------- GET

    @Nested
    @DisplayName("findAll")
    class FindAll {

        private final Pageable pageable = PageRequest.of(0, 2, Sort.by("sortOrder"));

        @Test
        @DisplayName("enriches names; allergy missing in ref-data -> name null; page metadata kept")
        void enrichesAndKeepsPageMetadata() {
            patientExists();
            when(patientAllergyRepository.findAllByIdPatientId(PATIENT_ID, pageable)).thenReturn(new PageImpl<>(
                    List.of(new PatientAllergy(patient, PEANUTS), new PatientAllergy(patient, MILK)), pageable, 5));
            refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));

            Page<PatientAllergyResponse> page = service.findAll(PATIENT_ID, pageable);

            assertThat(page.getContent())
                    .extracting(PatientAllergyResponse::allergyId, PatientAllergyResponse::name)
                    .containsExactly(tuple(PEANUTS, "Peanuts"), tuple(MILK, null));
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("asks ref-data only for the ids of the current page, in one call")
        void oneRefDataCallPerPage() {
            patientExists();
            when(patientAllergyRepository.findAllByIdPatientId(PATIENT_ID, pageable)).thenReturn(new PageImpl<>(
                    List.of(new PatientAllergy(patient, PEANUTS), new PatientAllergy(patient, MILK)), pageable, 5));
            refDataKnows();

            service.findAll(PATIENT_ID, pageable);

            verify(refDataClient).findAllergiesByIds(eq(new LinkedHashSet<>(List.of(PEANUTS, MILK))));
        }

        @Test
        @DisplayName("patient not found -> 404, no DB page query, no ref-data call")
        void patientNotFound() {
            when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.empty());

            assertStatus(catchThrowable(() -> service.findAll(PATIENT_ID, pageable)), HttpStatus.NOT_FOUND);
            verifyNoInteractions(patientAllergyRepository, refDataClient);
        }
    }

    // ---------------------------------------------------------------- PATCH

    @Nested
    @DisplayName("patch")
    class Patch {

        @Test
        @DisplayName("same id in create and delete -> 400 before touching DB or ref-data")
        void duplicateAcrossLists() {
            var request = new PatientAllergyPatchRequest(List.of(createItem(PEANUTS)), null, List.of(PEANUTS));

            assertStatus(catchThrowable(() -> service.patch(PATIENT_ID, request)), HttpStatus.BAD_REQUEST);
            verifyNoInteractions(patientRepository, patientAllergyRepository, refDataClient, writeService);
        }

        @Test
        @DisplayName("same id twice in create -> 400")
        void duplicateWithinList() {
            assertStatus(catchThrowable(() -> service.patch(PATIENT_ID, create(PEANUTS, PEANUTS))), HttpStatus.BAD_REQUEST);
            verifyNoInteractions(refDataClient, writeService);
        }

        @Test
        @DisplayName("patient not found -> 404 without ref-data call or write")
        void patientNotFound() {
            when(patientRepository.findByIdAndIsDeletedFalse(PATIENT_ID)).thenReturn(Optional.empty());

            assertStatus(catchThrowable(() -> service.patch(PATIENT_ID, create(PEANUTS))), HttpStatus.NOT_FOUND);
            verifyNoInteractions(refDataClient, writeService);
        }

        @Test
        @DisplayName("create of an already linked allergy -> 409 without ref-data call or write")
        void createAlreadyLinked() {
            patientExists();
            alreadyLinked(PEANUTS);

            assertStatus(catchThrowable(() -> service.patch(PATIENT_ID, create(PEANUTS))), HttpStatus.CONFLICT);
            verifyNoInteractions(refDataClient, writeService);
        }

        @Test
        @DisplayName("update of a not linked allergy -> 404 naming it, no write")
        void updateNotLinked() {
            patientExists();
            alreadyLinked();
            var request = new PatientAllergyPatchRequest(null, List.of(updateItem(MILK)), null);

            assertThatThrownBy(() -> service.patch(PATIENT_ID, request))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(e.getReason()).contains(MILK.toString());
                    });
            verifyNoInteractions(refDataClient, writeService);
        }

        @Test
        @DisplayName("delete of a not linked allergy -> 404, no write")
        void deleteNotLinked() {
            patientExists();
            alreadyLinked();
            var request = new PatientAllergyPatchRequest(null, null, List.of(MILK));

            assertStatus(catchThrowable(() -> service.patch(PATIENT_ID, request)), HttpStatus.NOT_FOUND);
            verifyNoInteractions(writeService);
        }

        @Test
        @DisplayName("unknown id in create -> 400 naming only the unknown id, no write")
        void unknownCreateId() {
            patientExists();
            alreadyLinked();
            refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));

            assertThatThrownBy(() -> service.patch(PATIENT_ID, create(PEANUTS, UNKNOWN)))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getReason()).contains(UNKNOWN.toString()).doesNotContain(PEANUTS.toString());
                    });
            verifyNoInteractions(writeService);
        }

        @Test
        @DisplayName("ref-data unavailable -> 503 propagates and nothing is written (call happens before the transaction)")
        void refDataUnavailable() {
            patientExists();
            alreadyLinked(MILK);
            when(refDataClient.findAllergiesByIds(anyCollection()))
                    .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ref-data-service is unavailable"));
            var request = new PatientAllergyPatchRequest(List.of(createItem(PEANUTS)), null, List.of(MILK));

            assertStatus(catchThrowable(() -> service.patch(PATIENT_ID, request)), HttpStatus.SERVICE_UNAVAILABLE);
            verifyNoInteractions(writeService);
        }

        @Test
        @DisplayName("only update/delete -> ref-data is not called at all")
        void noRefDataCallWithoutCreate() {
            patientExists();
            alreadyLinked(PEANUTS, MILK);
            var request = new PatientAllergyPatchRequest(null, List.of(updateItem(PEANUTS)), List.of(MILK));

            service.patch(PATIENT_ID, request);

            verifyNoInteractions(refDataClient);
            verify(writeService).apply(PATIENT_ID, request);
        }

        @Test
        @DisplayName("valid create + update + delete -> one write with the whole request")
        void validRequestIsWritten() {
            patientExists();
            alreadyLinked(PEANUTS, MILK);
            UUID eggs = UUID.fromString("8f1c0a6e-0b1a-4c1e-9a01-000000000002");
            refDataKnows(new RefDataAllergy(eggs, "Eggs"));
            var request = new PatientAllergyPatchRequest(List.of(createItem(eggs)), List.of(updateItem(PEANUTS)), List.of(MILK));

            service.patch(PATIENT_ID, request);

            verify(refDataClient).findAllergiesByIds(eq(new LinkedHashSet<>(List.of(eggs))));
            verify(writeService).apply(PATIENT_ID, request);
        }

        @Test
        @DisplayName("PK violation from a concurrent insert -> 409")
        void concurrentInsertIsConflict() {
            patientExists();
            alreadyLinked();
            refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));
            doThrow(new DataIntegrityViolationException("duplicate key"))
                    .when(writeService).apply(eq(PATIENT_ID), any());

            assertStatus(catchThrowable(() -> service.patch(PATIENT_ID, create(PEANUTS))), HttpStatus.CONFLICT);
        }
    }
}
