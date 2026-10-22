package com.patient.demo.patient;

import com.patient.demo.BaseIntegrationTest;
import com.patient.demo.refdata.dto.RefDataAllergy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the patient allergies REST resource against a real PostgreSQL instance.
 * ref-data-service is mocked (see {@code refDataClient} in {@link BaseIntegrationTest}).
 *
 * <p>Pins down that only allergy ids are stored, unknown ids are rejected on create and reads are
 * enriched with names coming from ref-data-service.
 */
class PatientAllergyControllerIT extends BaseIntegrationTest {

    private static final UUID PEANUTS = UUID.fromString("8f1c0a6e-0b1a-4c1e-9a01-000000000001");
    private static final UUID UNKNOWN = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientAllergyRepository patientAllergyRepository;

    private String url;

    @BeforeEach
    void setUp() {
        Patient patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDob(LocalDate.of(1990, 5, 17));
        patient.setGender(Gender.MALE);
        url = "/api/v1/patients/" + patientRepository.save(patient).getId() + "/allergies";
    }

    @AfterEach
    void cleanUp() {
        patientAllergyRepository.deleteAll();
        patientRepository.deleteAll();
    }

    private static String body(UUID allergyId) {
        return """
                {"allergies": [{"allergyId": "%s", "notes": "severe", "sortOrder": 0}]}
                """.formatted(allergyId);
    }

    private void refDataKnows(RefDataAllergy... allergies) {
        when(refDataClient.findAllergiesByIds(anyCollection())).thenReturn(List.of(allergies));
    }

    @Test
    @DisplayName("POST known id -> 201 with name; only the id is stored; GET returns it enriched")
    void createAndRead() throws Exception {
        refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body(PEANUTS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].allergyId").value(PEANUTS.toString()))
                .andExpect(jsonPath("$[0].name").value("Peanuts"))
                .andExpect(jsonPath("$[0].notes").value("severe"))
                .andExpect(jsonPath("$[0].createdBy").value("system"));

        assertThat(patientAllergyRepository.findAll())
                .singleElement()
                .satisfies(link -> assertThat(link.getAllergyId()).isEqualTo(PEANUTS));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Peanuts"));
    }

    @Test
    @DisplayName("GET after allergy disappeared from ref-data -> link kept with name null")
    void readWhenAllergyRemovedFromRefData() throws Exception {
        refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body(PEANUTS)))
                .andExpect(status().isCreated());

        refDataKnows();

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allergyId").value(PEANUTS.toString()))
                .andExpect(jsonPath("$[0].name").doesNotExist());
    }

    @Test
    @DisplayName("POST unknown id -> 400 naming the id, nothing stored")
    void unknownId() throws Exception {
        refDataKnows();

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body(UNKNOWN)))
                .andExpect(status().isBadRequest())
                // MockMvc does no /error dispatch, so there is no JSON error body here: assert the reason instead
                .andExpect(status().reason(containsString(UNKNOWN.toString())));

        assertThat(patientAllergyRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST without notes and sortOrder -> 201, sortOrder defaults to 0")
    void optionalFieldsOmitted() throws Exception {
        refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allergies\": [{\"allergyId\": \"%s\"}]}".formatted(PEANUTS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].sortOrder").value(0))
                .andExpect(jsonPath("$[0].notes").doesNotExist());
    }

    @Test
    @DisplayName("POST same allergy twice -> 409")
    void alreadyLinked() throws Exception {
        refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body(PEANUTS)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body(PEANUTS)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ref-data unavailable -> 503")
    void refDataDown() throws Exception {
        when(refDataClient.findAllergiesByIds(anyCollection()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ref-data-service is unavailable"));

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body(PEANUTS)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Empty list -> 400 (bean validation)")
    void emptyList() throws Exception {
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"allergies\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE linked -> 204, then not linked -> 404")
    void deleteLink() throws Exception {
        refDataKnows(new RefDataAllergy(PEANUTS, "Peanuts"));
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body(PEANUTS)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(url + "/" + PEANUTS)).andExpect(status().isNoContent());
        mockMvc.perform(delete(url + "/" + PEANUTS)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unknown patient -> 404")
    void unknownPatient() throws Exception {
        mockMvc.perform(get("/api/v1/patients/" + UUID.randomUUID() + "/allergies"))
                .andExpect(status().isNotFound());
    }
}
