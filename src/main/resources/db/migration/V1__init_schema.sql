-- =============================================================================
-- V1: Initial schema
-- =============================================================================

-- -----------------------------------------------------------------------------
-- patient
-- -----------------------------------------------------------------------------
CREATE TABLE patient
(
    id          UUID         NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    dob         DATE         NOT NULL,
    gender      VARCHAR(20)  NOT NULL,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    updated_by  VARCHAR(100) NOT NULL,

    CONSTRAINT pk_patient PRIMARY KEY (id),
    CONSTRAINT ck_patient_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN'))
);

-- Covers `GET /api/v1/patients`, which always filters on is_deleted = FALSE.
CREATE INDEX idx_patient_active
    ON patient (last_name, first_name)
    WHERE is_deleted = FALSE;


-- -----------------------------------------------------------------------------
-- allergy
-- -----------------------------------------------------------------------------
CREATE TABLE allergy
(
    id UUID NOT NULL,
    name varchar(255) CONSTRAINT uq_allergy_name UNIQUE NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    created_by varchar(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by varchar(100) NOT NULL,

    CONSTRAINT pk_allergy PRIMARY KEY (id)
);


-- -----------------------------------------------------------------------------
-- address
-- -----------------------------------------------------------------------------
CREATE TABLE address
(
    id UUID NOT NULL,
    patient_id UUID NOT NULL,
    address_type VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    line_1 VARCHAR(255) NOT NULL,
    line_2 VARCHAR(255),
    zip_code VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    created_by varchar(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by varchar(100) NOT NULL,

    CONSTRAINT pk_address PRIMARY KEY (id),
    CONSTRAINT ck_address_type CHECK (address_type IN ('PRIMARY','SECONDARY')),
    CONSTRAINT fk_address_patient FOREIGN KEY (patient_id) REFERENCES patient (id) ON DELETE CASCADE
);

CREATE INDEX idx_address_patient_id ON address (patient_id);


-- -----------------------------------------------------------------------------
-- patient_allergy
-- -----------------------------------------------------------------------------
CREATE TABLE patient_allergy
(
    patient_id UUID NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    allergy_id UUID NOT NULL REFERENCES allergy(id) ON DELETE RESTRICT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    notes VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL,
    created_by varchar(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by varchar(100) NOT NULL,

    CONSTRAINT pk_patient_allergy PRIMARY KEY (patient_id, allergy_id)
);

CREATE INDEX idx_patient_allergy_allergy_id ON patient_allergy (allergy_id);

