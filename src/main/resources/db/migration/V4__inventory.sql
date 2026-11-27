-- V4: inventory
-- BaseEntity tables: id, created_at, updated_at, version.
-- ReadOnlyEntity tables (append-only): id, created_at.

-- A label scheme is an ordered list of lines of parts. Templates (null organization) are
-- read-only; admins copy them into schemes of their organization.
CREATE TABLE label_scheme (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       REFERENCES organization (id),
    name             VARCHAR(255) NOT NULL,
    layout           JSONB        NOT NULL
);

CREATE UNIQUE INDEX uq_label_scheme_organization_name_lower
    ON label_scheme (organization_id, LOWER(name)) NULLS NOT DISTINCT;

-- The system needs both templates to work, so they are created here and not in a seed
INSERT INTO label_scheme (name, layout) VALUES
  ('Spring', '{"lines": [
     {"separator": "-", "parts": [
       {"part": "CATEGORY", "required": false, "editable": true},
       {"part": "TITLE", "required": true, "editable": true}]},
     {"separator": "", "parts": [{"part": "DEWEY", "required": false, "editable": true}]},
     {"separator": "", "parts": [{"part": "AUTHOR_MALAGA", "required": false, "editable": true}]},
     {"separator": "", "parts": [
       {"part": "COLLECTION_VOLUME", "required": false, "editable": true, "prefix": "v."}]},
     {"separator": "", "parts": [
       {"part": "SERIES_VOLUME", "required": false, "editable": true, "prefix": "t."}]},
     {"separator": "", "parts": [
       {"part": "COPY_NUMBER", "required": true, "editable": false, "prefix": "c."}]}]}'),
  ('Native', '{"lines": [
     {"separator": "-", "parts": [
       {"part": "CATEGORY", "required": true, "editable": false},
       {"part": "AUTHOR_LETTERS", "required": true, "editable": false},
       {"part": "SUBCATEGORY", "required": true, "editable": false},
       {"part": "TITLE", "required": true, "editable": false},
       {"part": "COPY_NUMBER", "required": true, "editable": false}]}]}');

CREATE TABLE library (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,
    institution_id  BIGINT       NOT NULL REFERENCES institution (id),
    name            VARCHAR(255) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    label_scheme_id BIGINT       NOT NULL REFERENCES label_scheme (id)
);

CREATE INDEX idx_library_label_scheme ON library (label_scheme_id);

CREATE UNIQUE INDEX uq_library_institution_name_lower ON library (institution_id, LOWER(name));

CREATE TABLE shelf (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    library_id  BIGINT       NOT NULL REFERENCES library (id),
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_shelf_library_id UNIQUE (library_id, id)
);

CREATE UNIQUE INDEX uq_shelf_library_name_lower ON shelf (library_id, LOWER(name));
CREATE UNIQUE INDEX uq_shelf_single_default ON shelf (library_id) WHERE is_default;

CREATE TABLE copy (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    library_id  BIGINT       NOT NULL REFERENCES library (id),
    book_id     BIGINT       NOT NULL REFERENCES book (id),
    shelf_id    BIGINT       NOT NULL REFERENCES shelf (id),
    iteration   INTEGER      NOT NULL,
    status      VARCHAR(50)  NOT NULL DEFAULT 'AVAILABLE',
    damaged_at  TIMESTAMPTZ,
    damaged_by_id BIGINT     REFERENCES account (id),
    transferred_from_copy_id BIGINT UNIQUE REFERENCES copy (id),
    notes       TEXT,
    CONSTRAINT ck_copy_status
        CHECK (status IN ('AVAILABLE', 'ON_LOAN', 'LOST', 'DISCARDED', 'TRANSFERRED')),
    CONSTRAINT ck_copy_damage_complete CHECK ((damaged_at IS NULL) = (damaged_by_id IS NULL)),
    CONSTRAINT ck_copy_iteration_positive CHECK (iteration > 0),
    CONSTRAINT fk_copy_shelf_library
        FOREIGN KEY (library_id, shelf_id)
        REFERENCES shelf (library_id, id),
    CONSTRAINT uq_copy_library_book_id UNIQUE (library_id, book_id, id)
);

CREATE UNIQUE INDEX uq_copy_library_book_iteration
    ON copy (library_id, book_id, iteration)
    WHERE status IN ('AVAILABLE', 'ON_LOAN');
CREATE INDEX idx_copy_book  ON copy (book_id);
CREATE INDEX idx_copy_shelf ON copy (shelf_id);

CREATE TABLE copy_condition_event (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    copy_id     BIGINT      NOT NULL REFERENCES copy (id),
    type        VARCHAR(50) NOT NULL,
    actor_id    BIGINT      NOT NULL REFERENCES account (id),
    reason      TEXT,
    CONSTRAINT ck_copy_condition_event_type CHECK (type IN ('DAMAGED', 'REPAIRED'))
);

CREATE INDEX idx_copy_condition_event_copy ON copy_condition_event (copy_id);

CREATE TABLE label (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    library_id        BIGINT       NOT NULL,
    book_id           BIGINT       NOT NULL,
    copy_id           BIGINT       NOT NULL,
    category_code     VARCHAR(255),
    dewey_code        VARCHAR(255),
    malaga_code       VARCHAR(255),
    title_code        VARCHAR(255),
    collection_volume INTEGER,
    series_volume     INTEGER,
    copy_number       INTEGER      NOT NULL,  
    text              VARCHAR(255) NOT NULL,
    print_status      VARCHAR(50)  NOT NULL DEFAULT 'PENDING_PRINT',
    printed_at        TIMESTAMPTZ,
    printed_by_id     BIGINT REFERENCES account (id),
    revision          INTEGER      NOT NULL DEFAULT 1,
    retired_at        TIMESTAMPTZ,            
    CONSTRAINT uq_label_copy UNIQUE (copy_id),
    CONSTRAINT fk_label_copy
        FOREIGN KEY (library_id, book_id, copy_id)
        REFERENCES copy (library_id, book_id, id),
    CONSTRAINT ck_label_print_status
        CHECK (print_status IN ('PENDING_PRINT', 'PRINTED', 'OUTDATED')),
    CONSTRAINT ck_label_copy_number_positive CHECK (copy_number > 0),
    CONSTRAINT ck_label_collection_volume_positive
        CHECK (collection_volume IS NULL OR collection_volume > 0),
    CONSTRAINT ck_label_series_volume_positive
        CHECK (series_volume IS NULL OR series_volume > 0),
    CONSTRAINT ck_label_revision_positive CHECK (revision > 0)
);

-- An OUTDATED label is a recovered one whose text is taken; it waits for a new text
CREATE UNIQUE INDEX uq_label_library_text_lower
    ON label (library_id, LOWER(text))
    WHERE retired_at IS NULL AND print_status <> 'OUTDATED';
CREATE UNIQUE INDEX uq_label_library_book_copy_number
    ON label (library_id, book_id, copy_number)
    WHERE retired_at IS NULL;
CREATE INDEX idx_label_pending_print
    ON label (library_id)
    WHERE print_status = 'PENDING_PRINT' AND retired_at IS NULL;
CREATE INDEX idx_label_printed_by ON label (printed_by_id);

CREATE TABLE label_revision (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    label_id          BIGINT       NOT NULL REFERENCES label (id) ON DELETE CASCADE,
    revision_number   INTEGER      NOT NULL,
    category_code     VARCHAR(255),
    dewey_code        VARCHAR(255),
    malaga_code       VARCHAR(255),
    title_code        VARCHAR(255),
    collection_volume INTEGER,
    series_volume     INTEGER,
    copy_number       INTEGER      NOT NULL,
    text              VARCHAR(255) NOT NULL,
    changed_by_id     BIGINT       NOT NULL REFERENCES account (id),
    change_reason     TEXT,
    CONSTRAINT uq_label_revision_number UNIQUE (label_id, revision_number)
);

CREATE INDEX idx_label_revision_changed_by ON label_revision (changed_by_id);

CREATE TABLE stock_movement (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    library_id   BIGINT      NOT NULL REFERENCES library (id),
    type         VARCHAR(50) NOT NULL,
    reason       VARCHAR(50) NOT NULL,
    signed_by_id BIGINT      NOT NULL REFERENCES account (id),
    notes        TEXT,
    CONSTRAINT ck_stock_movement_type CHECK (type IN ('ENTRY', 'EXIT')),
    CONSTRAINT ck_stock_movement_reason
        CHECK (reason IN ('PURCHASE', 'DONATION', 'RECOVERY', 'LOSS', 'DAMAGE',
                          'DISCARD', 'TRANSFER', 'OTHER'))
);

CREATE INDEX idx_stock_movement_library_date ON stock_movement (library_id, created_at DESC);
CREATE INDEX idx_stock_movement_signed_by    ON stock_movement (signed_by_id);

CREATE TABLE stock_movement_line (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    movement_id BIGINT      NOT NULL REFERENCES stock_movement (id),
    copy_id     BIGINT      NOT NULL REFERENCES copy (id),
    -- FK to loan_movement_line is added in V5
    loan_line_id      BIGINT UNIQUE,
    recovered_line_id BIGINT UNIQUE REFERENCES stock_movement_line (id),
    CONSTRAINT uq_stock_movement_line UNIQUE (movement_id, copy_id)
);

CREATE INDEX idx_stock_movement_line_copy ON stock_movement_line (copy_id);
