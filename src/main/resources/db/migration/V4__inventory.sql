-- V4: inventory module schema
-- BaseEntity tables: id, created_at, updated_at, version.
-- ReadOnlyEntity tables (append-only): id, created_at.

CREATE TABLE library (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uq_library_name_lower ON library (LOWER(name));

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
    notes       TEXT,
    CONSTRAINT ck_copy_status
        CHECK (status IN ('AVAILABLE', 'LOST', 'DISCARDED', 'TRANSFERRED')),
    CONSTRAINT ck_copy_iteration_positive CHECK (iteration > 0),
    CONSTRAINT fk_copy_shelf_library
        FOREIGN KEY (library_id, shelf_id)
        REFERENCES shelf (library_id, id),
    CONSTRAINT uq_copy_library_book_id UNIQUE (library_id, book_id, id)
);

CREATE UNIQUE INDEX uq_copy_library_book_iteration
    ON copy (library_id, book_id, iteration)
    WHERE status IN ('AVAILABLE', 'LOST');
CREATE INDEX idx_copy_book  ON copy (book_id);
CREATE INDEX idx_copy_shelf ON copy (shelf_id);

CREATE TABLE label (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    library_id        BIGINT       NOT NULL,
    book_id           BIGINT       NOT NULL,
    copy_id           BIGINT       NOT NULL,
    category_code     VARCHAR(255),
    collection_code   VARCHAR(255),
    dewey_code        VARCHAR(255) NOT NULL,
    malaga_source     VARCHAR(50)  NOT NULL,
    malaga_code       VARCHAR(255) NOT NULL,
    collection_volume INTEGER,
    series_volume     INTEGER,
    copy_number       INTEGER      NOT NULL,  -- snapshot of copy.iteration
    text              VARCHAR(255) NOT NULL,
    customized        BOOLEAN      NOT NULL DEFAULT FALSE,
    print_status      VARCHAR(50)  NOT NULL DEFAULT 'PENDING_PRINT',
    printed_at        TIMESTAMPTZ,
    printed_by_id     BIGINT REFERENCES account (id),
    revision          INTEGER      NOT NULL DEFAULT 1,
    retired_at        TIMESTAMPTZ,            
    CONSTRAINT uq_label_copy UNIQUE (copy_id),
    CONSTRAINT fk_label_copy
        FOREIGN KEY (library_id, book_id, copy_id)
        REFERENCES copy (library_id, book_id, id),
    CONSTRAINT ck_label_malaga_source
        CHECK (malaga_source IN ('AUTHOR', 'PUBLISHER')),
    CONSTRAINT ck_label_print_status
        CHECK (print_status IN ('PENDING_PRINT', 'PRINTED')),
    CONSTRAINT ck_label_copy_number_positive CHECK (copy_number > 0),
    CONSTRAINT ck_label_collection_volume_positive
        CHECK (collection_volume IS NULL OR collection_volume > 0),
    CONSTRAINT ck_label_series_volume_positive
        CHECK (series_volume IS NULL OR series_volume > 0),
    CONSTRAINT ck_label_revision_positive CHECK (revision > 0)
);

CREATE UNIQUE INDEX uq_label_library_text_lower
    ON label (library_id, LOWER(text))
    WHERE retired_at IS NULL;
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
    collection_code   VARCHAR(255),
    dewey_code        VARCHAR(255) NOT NULL,
    malaga_source     VARCHAR(50)  NOT NULL,
    malaga_code       VARCHAR(255) NOT NULL,
    collection_volume INTEGER,
    series_volume     INTEGER,
    copy_number       INTEGER      NOT NULL,
    text              VARCHAR(255) NOT NULL,
    changed_by_id     BIGINT       NOT NULL REFERENCES account (id),
    change_reason     TEXT,
    CONSTRAINT uq_label_revision_number UNIQUE (label_id, revision_number),
    CONSTRAINT ck_label_revision_malaga_source
        CHECK (malaga_source IN ('AUTHOR', 'PUBLISHER'))
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
        CHECK (reason IN ('PURCHASE', 'DONATION', 'LOSS', 'DAMAGE',
                          'DISCARD', 'TRANSFER', 'OTHER'))
);

CREATE INDEX idx_stock_movement_library_date ON stock_movement (library_id, created_at DESC);
CREATE INDEX idx_stock_movement_signed_by    ON stock_movement (signed_by_id);

CREATE TABLE stock_movement_line (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    movement_id BIGINT      NOT NULL REFERENCES stock_movement (id),
    copy_id     BIGINT      NOT NULL REFERENCES copy (id),
    CONSTRAINT uq_stock_movement_line UNIQUE (movement_id, copy_id)
);

CREATE INDEX idx_stock_movement_line_copy ON stock_movement_line (copy_id);
