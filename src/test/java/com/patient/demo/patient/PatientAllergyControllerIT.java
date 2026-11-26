package com.patient.demo.patient;

import com.patient.demo.BaseIntegrationTest;
import com.patient.demo.patient.dto.PatientAllergyCreateRequest;
import com.patient.demo.patient.dto.PatientAllergyPatchRequest;
import com.patient.demo.patient.service.PatientAllergyWriteService;
import com.patient.demo.refdata.dto.RefDataAllergy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the patient allergies REST resource against a real PostgreSQL instance.
 * ref-data-service is mocked (see {@code refDataClient} in {@link BaseIntegrationTest}).
 *
 * <p>Links needed as a precondition are inserted straight through the repository, so a test only
 * talks to ref-data when the behaviour under test does.
 *
 * <p>MockMvc does no {@code /error} dispatch, so error responses have no JSON body: the tests assert
 * the status and the {@code ResponseStatusException} reason instead.
 */
class PatientAllergyControllerIT extends BaseIntegrationTest {

    private static final UUID PEANUTS = UUID.fromString("8f1c0a6e-0b1a-4c1e-9a01-000000000001");
    private static final UUID EGGS = UUID.fromString("8f1c0a6e-0b1a-4c1e-9a01-000000000002");
    private static final UUID MILK = UUID.fromString("8f1c0a6e-0b1a-4c1e-9a01-000000000003");
    private static final UUID UNKNOWN = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientAllergyRepository patientAllergyRepository;

    @Autowired
    private PatientAllergyWriteService writeService;

    private Patient patient;
    private String url;

    @BeforeEach
    void setUp() {
        Patient newPatient = new Patient();
        newPatient.setFirstName("John");
        newPatient.setLastName("Doe");
        newPatient.setDob(LocalDate.of(1990, 5, 17));
        newPatient.setGender(Gender.MALE);
        patient = patientRepository.save(newPatient);
        url = "/api/v1/patients/" + patient.getId() + "/allergies";
    }

    @AfterEach
    void cleanUp() {
        patientAllergyRepository.deleteAll();
        patientRepository.deleteAll();
    }

    // ---------------------------------------------------------------- helpers

    private void link(UUID allergyId, int sortOrder, String notes) {
        PatientAllergy link = new PatientAllergy(patient, allergyId);
        link.setSortOrder(sortOrder);
        link.setNotes(notes);
        patientAllergyRepository.save(link);
    }

    private void refDataKnows(RefDataAllergy... allergies) {
        when(refDataClient.findAllergiesByIds(anyCollection())).thenReturn(List.of(allergies));
    }

    private ResultActions patchWith(String json) throws Exception {
        return mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private List<UUID> linkedIds() {
        return patientAllergyRepository.findAll().stream().map(PatientAllergy::getAllergyId).toList();
    }

    // ---------------------------------------------------------------- GET

    @Nested
    @DisplayName("GET")
    class Get {

        @Test
        @DisplayName("returns a page enriched with names from ref-data, sorted by sortOrder by default")
        void pageEnrichedAndSorted() throws Exception {
            link(MILK, 2, null);
            link(PEANUTS, 0, "severe");
            link(EGGS, 1, null);
            refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"), new RefDataAllergy(EGGS, "Eggs"),
                    new RefDataAllergy(MILK, "Milk"));

            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].name", contains("Peanuts", "Eggs", "Milk")))
                    .andExpect(jsonPath("$.content[0].allergyId").value(PEANUTS.toString()))
                    .andExpect(jsonPath("$.content[0].notes").value("severe"))
                    .andExpect(jsonPath("$.content[0].createdBy").value("system"))
                    .andExpect(jsonPath("$.page.totalElements").value(3));
        }

        @Test
        @DisplayName("size and page split the result; metadata says how many there are")
        void pagination() throws Exception {
            link(PEANUTS, 0, null);
            link(EGGS, 1, null);
            link(MILK, 2, null);
            refDataKnows();

            mockMvc.perform(get(url).param("size", "2").param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].allergyId", contains(PEANUTS.toString(), EGGS.toString())))
                    .andExpect(jsonPath("$.page.size").value(2))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$.page.totalElements").value(3))
                    .andExpect(jsonPath("$.page.totalPages").value(2));

            mockMvc.perform(get(url).param("size", "2").param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].allergyId", contains(MILK.toString())));
        }

        @Test
        @DisplayName("sort=sortOrder,desc reverses the order")
        void sortDescending() throws Exception {
            link(PEANUTS, 0, null);
            link(MILK, 1, null);
            refDataKnows();

            mockMvc.perform(get(url).param("sort", "sortOrder,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].allergyId", contains(MILK.toString(), PEANUTS.toString())));
        }

        @Test
        @DisplayName("sort by a property outside the whitelist -> 400, not 500")
        void unsortableProperty() throws Exception {
            mockMvc.perform(get(url).param("sort", "hack"))
                    .andExpect(status().isBadRequest())
                    .andExpect(status().reason(containsString("hack")));
        }

        @Test
        @DisplayName("size above the limit is capped at 100")
        void maxPageSize() throws Exception {
            refDataKnows();

            mockMvc.perform(get(url).param("size", "5000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.size").value(100));
        }

        @Test
        @DisplayName("allergy disappeared from ref-data -> link kept with name null")
        void allergyRemovedFromRefData() throws Exception {
            link(PEANUTS, 0, null);
            refDataKnows();

            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].allergyId").value(PEANUTS.toString()))
                    .andExpect(jsonPath("$.content[0].name").doesNotExist());
        }

        @Test
        @DisplayName("ref-data unavailable -> 503")
        void refDataDown() throws Exception {
            link(PEANUTS, 0, null);
            when(refDataClient.findAllergiesByIds(anyCollection()))
                    .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ref-data-service is unavailable"));

            mockMvc.perform(get(url)).andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("unknown patient -> 404")
        void unknownPatient() throws Exception {
            mockMvc.perform(get("/api/v1/patients/" + UUID.randomUUID() + "/allergies"))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------- PATCH: happy path

    @Nested
    @DisplayName("PATCH")
    class Patch {

        @Test
        @DisplayName("create -> 204; GET returns the new link with its name")
        void createThenRead() throws Exception {
            refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));

            patchWith("""
                    {"create": [{"allergyId": "%s", "notes": "severe", "sortOrder": 3}]}
                    """.formatted(PEANUTS))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Peanuts"))
                    .andExpect(jsonPath("$.content[0].notes").value("severe"))
                    .andExpect(jsonPath("$.content[0].sortOrder").value(3));
        }

        @Test
        @DisplayName("create without notes and sortOrder -> sortOrder defaults to 0")
        void optionalFieldsOmitted() throws Exception {
            refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));

            patchWith("""
                    {"create": [{"allergyId": "%s"}]}
                    """.formatted(PEANUTS))
                    .andExpect(status().isNoContent());

            PatientAllergy stored = patientAllergyRepository.findAll().get(0);
            assertThat(stored.getSortOrder()).isZero();
            assertThat(stored.getNotes()).isNull();
        }

        @Test
        @DisplayName("create + update + delete in one request -> all applied")
        void allThreeOperations() throws Exception {
            link(PEANUTS, 0, "old");
            link(MILK, 1, null);
            refDataKnows(new RefDataAllergy(EGGS, "Eggs"));

            patchWith("""
                    {
                      "create": [{"allergyId": "%s", "sortOrder": 5}],
                      "update": [{"allergyId": "%s", "notes": "new", "sortOrder": 7}],
                      "delete": ["%s"]
                    }
                    """.formatted(EGGS, PEANUTS, MILK))
                    .andExpect(status().isNoContent());

            assertThat(linkedIds()).containsExactlyInAnyOrder(PEANUTS, EGGS);
            PatientAllergy peanuts = patientAllergyRepository.findById(new PatientAllergyId(patient.getId(), PEANUTS)).orElseThrow();
            assertThat(peanuts.getNotes()).isEqualTo("new");
            assertThat(peanuts.getSortOrder()).isEqualTo(7);
        }

        @Test
        @DisplayName("update with only notes keeps sortOrder; empty notes clears them")
        void partialUpdate() throws Exception {
            link(PEANUTS, 4, "old");

            patchWith("""
                    {"update": [{"allergyId": "%s", "notes": ""}]}
                    """.formatted(PEANUTS))
                    .andExpect(status().isNoContent());

            PatientAllergy peanuts = patientAllergyRepository.findAll().get(0);
            assertThat(peanuts.getNotes()).isNull();
            assertThat(peanuts.getSortOrder()).isEqualTo(4);
        }

        @Test
        @DisplayName("update/delete only -> ref-data is never called")
        void noRefDataCallWithoutCreate() throws Exception {
            link(PEANUTS, 0, null);
            link(MILK, 1, null);

            patchWith("""
                    {"update": [{"allergyId": "%s", "sortOrder": 9}], "delete": ["%s"]}
                    """.formatted(PEANUTS, MILK))
                    .andExpect(status().isNoContent());

            verifyNoInteractions(refDataClient);
            assertThat(linkedIds()).containsExactly(PEANUTS);
        }
    }

    // ---------------------------------------------------------------- PATCH: rejected requests

    @Nested
    @DisplayName("PATCH rejected")
    class PatchRejected {

        @Test
        @DisplayName("empty body -> 400")
        void emptyBody() throws Exception {
            patchWith("{}").andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("create item without allergyId -> 400")
        void missingAllergyId() throws Exception {
            patchWith("""
                    {"create": [{"notes": "x"}]}
                    """).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("notes longer than the column -> 400, not a DB error")
        void notesTooLong() throws Exception {
            patchWith("""
                    {"create": [{"allergyId": "%s", "notes": "%s"}]}
                    """.formatted(PEANUTS, "x".repeat(1001)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("update item that changes nothing -> 400")
        void updateWithoutChanges() throws Exception {
            link(PEANUTS, 0, null);

            patchWith("""
                    {"update": [{"allergyId": "%s"}]}
                    """.formatted(PEANUTS))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("same id in create and delete -> 400, nothing changed")
        void sameIdInTwoLists() throws Exception {
            link(PEANUTS, 0, null);

            patchWith("""
                    {"create": [{"allergyId": "%s"}], "delete": ["%s"]}
                    """.formatted(PEANUTS, PEANUTS))
                    .andExpect(status().isBadRequest());

            assertThat(linkedIds()).containsExactly(PEANUTS);
        }

        @Test
        @DisplayName("unknown id -> 400 naming the id, nothing stored")
        void unknownId() throws Exception {
            refDataKnows();

            patchWith("""
                    {"create": [{"allergyId": "%s"}]}
                    """.formatted(UNKNOWN))
                    .andExpect(status().isBadRequest())
                    .andExpect(status().reason(containsString(UNKNOWN.toString())));

            assertThat(linkedIds()).isEmpty();
        }

        @Test
        @DisplayName("create of an already linked allergy -> 409")
        void alreadyLinked() throws Exception {
            link(PEANUTS, 0, null);

            patchWith("""
                    {"create": [{"allergyId": "%s"}]}
                    """.formatted(PEANUTS))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("update or delete of a not linked allergy -> 404")
        void notLinked() throws Exception {
            patchWith("""
                    {"update": [{"allergyId": "%s", "notes": "x"}]}
                    """.formatted(PEANUTS))
                    .andExpect(status().isNotFound());

            patchWith("""
                    {"delete": ["%s"]}
                    """.formatted(PEANUTS))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("unknown patient -> 404")
        void unknownPatient() throws Exception {
            mockMvc.perform(patch("/api/v1/patients/" + UUID.randomUUID() + "/allergies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"delete": ["%s"]}
                                    """.formatted(PEANUTS)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ref-data down -> 503 and the delete in the same request is NOT applied (atomic)")
        void refDataDownIsAtomic() throws Exception {
            link(PEANUTS, 0, null);
            when(refDataClient.findAllergiesByIds(anyCollection()))
                    .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ref-data-service is unavailable"));

            patchWith("""
                    {"create": [{"allergyId": "%s"}], "delete": ["%s"]}
                    """.formatted(MILK, PEANUTS))
                    .andExpect(status().isServiceUnavailable());

            assertThat(linkedIds()).containsExactly(PEANUTS);
        }
    }

    // ---------------------------------------------------------------- writer: concurrency

    @Nested
    @DisplayName("writer")
    class Writer {

        /**
         * Simulates the race the service checks cannot close: another request inserted the same link
         * between our "not linked yet" check and our insert. The writer must fail on the primary key
         * (persist), not silently overwrite the other request's data (merge).
         */
        @Test
        @DisplayName("insert of a link that already exists fails on the PK instead of overwriting it")
        void concurrentInsertDoesNotOverwrite() {
            link(PEANUTS, 0, "written by the other request");
            var request = new PatientAllergyPatchRequest(
                    List.of(new PatientAllergyCreateRequest(PEANUTS, "ours", 1)), null, null);

            assertThatThrownBy(() -> writeService.apply(patient.getId(), request))
                    .isInstanceOf(DataIntegrityViolationException.class);

            PatientAllergy stored = patientAllergyRepository.findAll().get(0);
            assertThat(stored.getNotes()).isEqualTo("written by the other request");
            assertThat(stored.getSortOrder()).isZero();
        }
    }
}
