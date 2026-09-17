package com.patient.demo;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: verifies that the Spring context starts, Flyway applies every migration and
 * Hibernate's {@code ddl-auto=validate} agrees that all entity mappings match the schema
 * created by Flyway.
 */
class PatientDemoApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }
}
