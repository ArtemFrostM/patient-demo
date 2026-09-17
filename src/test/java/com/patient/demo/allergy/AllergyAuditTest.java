package com.patient.demo.allergy;

import com.patient.demo.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the JPA auditing wiring ({@code AuditableEntity} + {@code JpaAuditingConfig}).
 *
 * <p>Deliberately <strong>not</strong> {@code @Transactional}: every repository call must run in
 * its own transaction with its own persistence context, otherwise the first-level cache would
 * hand back the same in-memory instance and we would assert against values that were never
 * written to (or read from) PostgreSQL.
 */
class AllergyAuditTest extends BaseIntegrationTest {

    private static final String EXPECTED_AUDITOR = "system";

    @Autowired
    private AllergyRepository allergyRepository;

    @AfterEach
    void cleanUp() {
        allergyRepository.deleteAll();
    }

    @Test
    @DisplayName("INSERT fills all four audit columns")
    void shouldPopulateAuditFieldsOnCreate() {
        UUID id = allergyRepository.save(allergyNamed("Penicillin")).getId();

        Allergy created = findRequired(id);

        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
        assertThat(created.getCreatedBy()).isEqualTo(EXPECTED_AUDITOR);
        assertThat(created.getUpdatedBy()).isEqualTo(EXPECTED_AUDITOR);
    }

    @Test
    @DisplayName("A freshly created row has createdAt == updatedAt")
    void shouldUseTheSameTimestampForBothColumnsOnCreate() {
        UUID id = allergyRepository.save(allergyNamed("Latex")).getId();

        Allergy created = findRequired(id);

        assertThat(created.getUpdatedAt()).isEqualTo(created.getCreatedAt());
    }

    @Test
    @DisplayName("UPDATE advances updatedAt but never touches createdAt/createdBy")
    void shouldOnlyAdvanceModificationAuditOnUpdate() {
        UUID id = allergyRepository.save(allergyNamed("Aspirin")).getId();
        Allergy created = findRequired(id);

        created.setName("Acetylsalicylic acid");
        allergyRepository.save(created);

        Allergy updated = findRequired(id);

        assertThat(updated.getName()).isEqualTo("Acetylsalicylic acid");
        // `updatable = false` must keep the creation audit frozen ...
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getCreatedBy()).isEqualTo(created.getCreatedBy());
        // ... while the modification audit moves forward.
        assertThat(updated.getUpdatedAt()).isAfter(created.getUpdatedAt());
        assertThat(updated.getUpdatedBy()).isEqualTo(EXPECTED_AUDITOR);
    }

    private Allergy allergyNamed(String name) {
        Allergy allergy = new Allergy();
        allergy.setName(name);
        return allergy;
    }

    /** Reads the row back through a fresh persistence context, i.e. the real database state. */
    private Allergy findRequired(UUID id) {
        return allergyRepository.findById(id).orElseThrow();
    }
}
