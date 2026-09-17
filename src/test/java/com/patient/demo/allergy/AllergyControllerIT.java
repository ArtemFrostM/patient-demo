package com.patient.demo.allergy;

import com.patient.demo.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
 * End-to-end test of the allergy REST resource: real HTTP mapping, real validation, real Hibernate,
 * real PostgreSQL (Testcontainers). Only the servlet container itself is mocked away by MockMvc.
 *
 * <p>The database is shared across the whole Gradle test run, therefore every test cleans up after
 * itself in {@link #cleanUp()}.
 */
class AllergyControllerIT extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/allergies";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AllergyRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("POST creates the row, answers 201 and echoes the audit columns")
    void shouldCreateAllergy() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Peanut"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Peanut"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.createdBy").value("system"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedBy").value("system"));

        assertThat(repository.findAll()).singleElement()
                .extracting(Allergy::getName)
                .isEqualTo("Peanut");
    }

    @Test
    @DisplayName("POST with a blank name is rejected with 400 and nothing is persisted")
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("POST with a malformed body is rejected with 400")
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /{id} returns the stored row")
    void shouldReturnSingleAllergy() throws Exception {
        UUID id = persist("Latex");

        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Latex"));
    }

    @Test
    @DisplayName("GET /{id} answers 404 for an unknown id")
    void shouldAnswerNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{id} answers 400 when the id is not a UUID")
    void shouldAnswerBadRequestForMalformedId() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET returns a sorted, paginated slice together with the total count")
    void shouldReturnPagedAndSortedList() throws Exception {
        persist("Latex");
        persist("Aspirin");
        persist("Peanut");

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                // @PageableDefault(sort = "name") must apply even when the client omits `sort`
                .andExpect(jsonPath("$.content[0].name").value("Aspirin"))
                .andExpect(jsonPath("$.content[1].name").value("Latex"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("PUT renames the row, advances updatedAt and freezes createdAt")
    void shouldUpdateAllergy() throws Exception {
        UUID id = persist("Aspirin");
        Allergy before = repository.findById(id).orElseThrow();

        mockMvc.perform(put(BASE_URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Acetylsalicylic acid"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Acetylsalicylic acid"));

        Allergy after = repository.findById(id).orElseThrow();
        assertThat(after.getName()).isEqualTo("Acetylsalicylic acid");
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
        assertThat(after.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
    }

    @Test
    @DisplayName("PUT answers 404 for an unknown id")
    void shouldAnswerNotFoundWhenUpdatingUnknownId() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Anything"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE removes the row and answers 204; reading it afterwards yields 404")
    void shouldDeleteAllergy() throws Exception {
        UUID id = persist("Pollen");

        mockMvc.perform(delete(BASE_URL + "/{id}", id))
                .andExpect(status().isNoContent());

        assertThat(repository.existsById(id)).isFalse();

        mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE answers 404 for an unknown id instead of pretending success")
    void shouldAnswerNotFoundWhenDeletingUnknownId() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // TODO Step 1: once GlobalExceptionHandler maps DataIntegrityViolationException, assert 409 here.
    // Right now a duplicate `name` escapes as an unhandled exception and surfaces as 500.

    private UUID persist(String name) {
        Allergy allergy = new Allergy();
        allergy.setName(name);
        return repository.saveAndFlush(allergy).getId();
    }
}



