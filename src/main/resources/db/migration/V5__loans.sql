-- V5: loan 

CREATE TABLE visitor_role (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    max_days    INTEGER      NOT NULL,
    CONSTRAINT uq_visitor_role_code UNIQUE (code),
    CONSTRAINT ck_visitor_role_max_days_positive CHECK (max_days > 0)
);

CREATE UNIQUE INDEX uq_visitor_role_name_lower ON visitor_role (LOWER(name));

CREATE TABLE school_grade (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    level       VARCHAR(50)  NOT NULL,
    grade       INTEGER      NOT NULL,
    CONSTRAINT uq_school_grade_level_grade UNIQUE (level, grade),
    CONSTRAINT ck_school_grade_level CHECK (level IN ('PRIMARY', 'SECONDARY')),
    CONSTRAINT ck_school_grade_range CHECK (
        (level = 'PRIMARY'   AND grade BETWEEN 1 AND 6)
     OR (level = 'SECONDARY' AND grade BETWEEN 1 AND 5))
);

CREATE TABLE visitor (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(255) NOT NULL,
    role_id     BIGINT       NOT NULL REFERENCES visitor_role (id),
    grade_id    BIGINT       REFERENCES school_grade (id),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_visitor_role  ON visitor (role_id);
CREATE INDEX idx_visitor_grade ON visitor (grade_id);
CREATE UNIQUE INDEX uq_visitor_grade_name_lower
    ON visitor (grade_id, LOWER(name)) NULLS NOT DISTINCT
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
