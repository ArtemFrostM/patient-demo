-- =============================================================================
-- V2: Allergy migration
-- =============================================================================

ALTER TABLE patient_allergy DROP CONSTRAINT patient_allergy_allergy_id_fkey;

DROP TABLE allergy;