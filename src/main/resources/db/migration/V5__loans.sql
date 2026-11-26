-- V5: loan

-- Rows without an institution are the organization's templates, copied into each new institution.
CREATE TABLE visitor_role (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                  BIGINT       NOT NULL DEFAULT 0,
    organization_id          BIGINT       NOT NULL REFERENCES organization (id),
    institution_id           BIGINT,
    name                     VARCHAR(255) NOT NULL,
    requires_grade           BOOLEAN      NOT NULL DEFAULT FALSE,
    max_days                 INTEGER      NOT NULL,
    max_active_loans         INTEGER      NOT NULL DEFAULT 5,
    overdue_block_threshold  INTEGER      NOT NULL DEFAULT 1,
    grace_days               INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_visitor_role_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id),
    CONSTRAINT uq_visitor_role_institution_id UNIQUE (institution_id, id),
    CONSTRAINT ck_visitor_role_max_days_positive CHECK (max_days > 0),
    CONSTRAINT ck_visitor_role_max_active_loans_positive CHECK (max_active_loans > 0),
    CONSTRAINT ck_visitor_role_overdue_block_threshold_positive CHECK (overdue_block_threshold > 0),
    CONSTRAINT ck_visitor_role_grace_days_not_negative CHECK (grace_days >= 0)
);

CREATE UNIQUE INDEX uq_visitor_role_scope_name_lower
    ON visitor_role (organization_id, institution_id, LOWER(name)) NULLS NOT DISTINCT;

CREATE TABLE school_grade (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    institution_id   BIGINT,
    level            VARCHAR(50)  NOT NULL,
    grade            INTEGER      NOT NULL,
    CONSTRAINT fk_school_grade_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id),
    CONSTRAINT uq_school_grade_institution_id UNIQUE (institution_id, id),
    CONSTRAINT uq_school_grade_scope_level_grade
        UNIQUE NULLS NOT DISTINCT (organization_id, institution_id, level, grade),
    CONSTRAINT ck_school_grade_level CHECK (level IN ('PRIMARY', 'SECONDARY')),
    CONSTRAINT ck_school_grade_range CHECK (
        (level = 'PRIMARY'   AND grade BETWEEN 1 AND 6)
     OR (level = 'SECONDARY' AND grade BETWEEN 1 AND 5))
);

-- The composite keys keep a visitor's role and grade inside the visitor's institution.
CREATE TABLE visitor (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,
    institution_id  BIGINT       NOT NULL REFERENCES institution (id),
    name            VARCHAR(255) NOT NULL,
    role_id         BIGINT       NOT NULL,
    grade_id        BIGINT,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_visitor_role FOREIGN KEY (institution_id, role_id)
        REFERENCES visitor_role (institution_id, id),
    CONSTRAINT fk_visitor_grade FOREIGN KEY (institution_id, grade_id)
        REFERENCES school_grade (institution_id, id)
);

CREATE INDEX idx_visitor_institution ON visitor (institution_id);
CREATE INDEX idx_visitor_role  ON visitor (role_id);
CREATE INDEX idx_visitor_grade ON visitor (grade_id);
CREATE UNIQUE INDEX uq_visitor_grade_name_lower
    ON visitor (institution_id, grade_id, LOWER(name)) NULLS NOT DISTINCT
    WHERE active;

CREATE TABLE loan_movement (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT      NOT NULL DEFAULT 0,
    library_id   BIGINT      NOT NULL REFERENCES library (id),
    loaned_by_id BIGINT      NOT NULL REFERENCES account (id),
    loaned_to_id BIGINT      NOT NULL REFERENCES visitor (id),
    expires_by   TIMESTAMPTZ NOT NULL,
    notes        TEXT,
    CONSTRAINT ck_loan_movement_expires_after_creation CHECK (expires_by > created_at)
);

CREATE INDEX idx_loan_movement_library_date ON loan_movement (library_id, created_at DESC);
CREATE INDEX idx_loan_movement_loaned_to    ON loan_movement (loaned_to_id);
CREATE INDEX idx_loan_movement_loaned_by    ON loan_movement (loaned_by_id);

CREATE TABLE loan_movement_line (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT      NOT NULL DEFAULT 0,
    loan_movement_id BIGINT      NOT NULL REFERENCES loan_movement (id),
    copy_id          BIGINT      NOT NULL REFERENCES copy (id),
    resolved_at      TIMESTAMPTZ,
    resolved_by_id   BIGINT      REFERENCES account (id),
    outcome          VARCHAR(50),
    CONSTRAINT uq_loan_movement_line UNIQUE (loan_movement_id, copy_id),
    CONSTRAINT ck_loan_line_outcome
        CHECK (outcome IN ('RETURNED', 'RETURNED_DAMAGED', 'LOST')),
    CONSTRAINT ck_loan_line_resolution_complete CHECK (
        (resolved_at IS NULL AND resolved_by_id IS NULL AND outcome IS NULL)
     OR (resolved_at IS NOT NULL AND resolved_by_id IS NOT NULL AND outcome IS NOT NULL))
);

CREATE UNIQUE INDEX uq_loan_line_open_copy
    ON loan_movement_line (copy_id)
    WHERE resolved_at IS NULL;
CREATE INDEX idx_loan_line_copy        ON loan_movement_line (copy_id);
CREATE INDEX idx_loan_line_resolved_by ON loan_movement_line (resolved_by_id);
