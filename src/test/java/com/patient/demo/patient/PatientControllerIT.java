package com.patient.demo.patient;

import com.patient.demo.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the patient REST resource against a real PostgreSQL instance.
 *
 * <p>Beyond plain CRUD this pins down the <em>soft delete</em> contract: after {@code DELETE} the
 * row must still be physically present while being invisible through every public read path.
 */
class PatientControllerIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/patients";
    private static final LocalDate DOB = LocalDate.of(1990, 5, 17);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("POST creates the row, answers 201 and never exposes the isDeleted flag")
    void shouldCreatePatient() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "middleName": "Quincy",
                                  "dob": "1990-05-17",
                                  "gender": "MALE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.middleName").value("Quincy"))
                .andExpect(jsonPath("$.dob").value("1990-05-17"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.createdBy").value("system"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.isDeleted").doesNotExist())
                .andExpect(jsonPath("$.deleted").doesNotExist());

        assertThat(repository.findAll()).singleElement()
                .satisfies(patient -> assertThat(patient.isDeleted()).isFalse());
    }

    @Test
    @DisplayName("POST accepts an absent middleName, which is optional in the schema")
    void shouldCreatePatientWithoutMiddleName() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Jane", "lastName": "Roe", "dob": "1985-01-02", "gender": "FEMALE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.middleName").doesNotExist());
    }

    @Test
    @DisplayName("POST with a blank firstName is rejected with 400")
    void shouldRejectBlankFirstName() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "  ", "lastName": "Doe", "dob": "1990-05-17", "gender": "MALE"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("POST without dob is rejected with 400, not 500")
    void shouldRejectMissingDob() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "John", "lastName": "Doe", "gender": "MALE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST without gender is rejected with 400, not 500")
    void shouldRejectMissingGender() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "John", "lastName": "Doe", "dob": "1990-05-17"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST with a dob in the future is rejected with 400")
    void shouldRejectFutureDob() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "John", "lastName": "Doe", "dob": "2999-01-01", "gender": "MALE"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("POST with an unknown gender literal is rejected with 400")
    void shouldRejectUnknownGender() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "John", "lastName": "Doe", "dob": "1990-05-17", "gender": "MARTIAN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ read

    @Test
    @DisplayName("GET /{id} returns the stored patient")
    void shouldReturnSinglePatient() throws Exception {
        UUID id = persist("Jane", "Roe");

        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Roe"));
    }

    @Test
    @DisplayName("GET /{id} answers 404 for an unknown id")
    void shouldAnswerNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET returns a slice sorted by lastName together with the total count")
    void shouldReturnPagedAndSortedList() throws Exception {
        persist("John", "Zulu");
        persist("Jane", "Alpha");
        persist("Jim", "Mike");

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                // @PageableDefault(sort = "lastName") must apply even when the client omits `sort`
                .andExpect(jsonPath("$.content[0].lastName").value("Alpha"))
                .andExpect(jsonPath("$.content[1].lastName").value("Mike"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    // ---------------------------------------------------------------- update

    @Test
    @DisplayName("PUT overwrites the editable fields, freezes createdAt and keeps isDeleted false")
    void shouldUpdatePatient() throws Exception {
        UUID id = persist("Old", "Name");
        Patient before = repository.findById(id).orElseThrow();

        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "New",
                                  "lastName": "Surname",
                                  "middleName": null,
                                  "dob": "1975-12-31",
                                  "gender": "OTHER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("Surname"))
                .andExpect(jsonPath("$.dob").value("1975-12-31"))
                .andExpect(jsonPath("$.gender").value("OTHER"));

        Patient after = repository.findById(id).orElseThrow();
        assertThat(after.getMiddleName()).isNull();
        assertThat(after.isDeleted()).isFalse();
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
        assertThat(after.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
    }

    @Test
    @DisplayName("PUT answers 404 for an unknown id")
    void shouldAnswerNotFoundWhenUpdatingUnknownId() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "A", "lastName": "B", "dob": "1990-05-17", "gender": "MALE"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------- soft delete

    @Test
    @DisplayName("DELETE answers 204 and keeps the row physically present with isDeleted = true")
    void shouldSoftDeletePatient() throws Exception {
        UUID id = persist("Mark", "Twain");

        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isNoContent());

        // The row must survive — this is what makes it a *soft* delete.
        assertThat(repository.findById(id)).hasValueSatisfying(
                patient -> assertThat(patient.isDeleted()).isTrue());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("A soft-deleted patient disappears from GET /{id}")
    void shouldHideSoftDeletedPatientFromSingleRead() throws Exception {
        UUID id = persist("Mark", "Twain");

        mockMvc.perform(delete(BASE_URL + "/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A soft-deleted patient disappears from the list and from totalElements")
    void shouldHideSoftDeletedPatientFromList() throws Exception {
        UUID deletedId = persist("Gone", "Away");
        persist("Still", "Here");

        mockMvc.perform(delete(BASE_URL + "/{id}", deletedId)).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].lastName").value("Here"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Deleting twice answers 404 the second time instead of pretending success")
    void shouldAnswerNotFoundOnRepeatedDelete() throws Exception {
        UUID id = persist("Mark", "Twain");

        mockMvc.perform(delete(BASE_URL + "/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A soft-deleted patient cannot be resurrected through PUT")
    void shouldRefuseToUpdateSoftDeletedPatient() throws Exception {
        UUID id = persist("Mark", "Twain");

        mockMvc.perform(delete(BASE_URL + "/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Back", "lastName": "Again", "dob": "1990-05-17", "gender": "MALE"}
                                """))
                .andExpect(status().isNotFound());

        assertThat(repository.findById(id)).hasValueSatisfying(
                patient -> assertThat(patient.isDeleted()).isTrue());
    }

    @Test
    @DisplayName("DELETE answers 404 for an unknown id")
    void shouldAnswerNotFoundWhenDeletingUnknownId() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private UUID persist(String firstName, String lastName) {
        Patient patient = new Patient();
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDob(DOB);
        patient.setGender(Gender.MALE);
        return repository.saveAndFlush(patient).getId();
    }
}

