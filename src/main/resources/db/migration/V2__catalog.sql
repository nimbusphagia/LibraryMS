-- V2: catalog
-- Every table mirrors BaseEntity: id, created_at, updated_at, version.
-- Catalog rows belong to an organization. A null institution_id is the shared catalog,
-- managed by admins; otherwise the row is exclusive to that institution.

-- Accent-insensitive search
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE author (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    institution_id   BIGINT,
    first_name       VARCHAR(255) NOT NULL,
    last_name        VARCHAR(255),
    CONSTRAINT fk_author_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id)
);

CREATE UNIQUE INDEX uq_author_scope_full_name
    ON author (organization_id, institution_id, LOWER(first_name), LOWER(last_name))
    NULLS NOT DISTINCT;

CREATE TABLE publisher (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    institution_id   BIGINT,
    name             VARCHAR(255) NOT NULL,
    CONSTRAINT fk_publisher_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id)
);

CREATE UNIQUE INDEX uq_publisher_scope_name_lower
    ON publisher (organization_id, institution_id, LOWER(name)) NULLS NOT DISTINCT;

CREATE TABLE collection (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    institution_id   BIGINT,
    name             VARCHAR(255) NOT NULL,
    code             VARCHAR(255),
    publisher_id     BIGINT       NOT NULL REFERENCES publisher (id),
    CONSTRAINT fk_collection_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id),
    CONSTRAINT uq_collection_publisher_id UNIQUE (publisher_id, id)
);

CREATE UNIQUE INDEX uq_collection_scope_publisher_name_lower
    ON collection (publisher_id, institution_id, LOWER(name)) NULLS NOT DISTINCT;

-- COALESCE instead of NULLS NOT DISTINCT so collections without a code don't collide
CREATE UNIQUE INDEX uq_collection_scope_publisher_code_lower
    ON collection (publisher_id, COALESCE(institution_id, 0), LOWER(code));

CREATE TABLE series (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    institution_id   BIGINT,
    name             VARCHAR(255) NOT NULL,
    author_id        BIGINT       NOT NULL REFERENCES author (id),
    CONSTRAINT fk_series_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id),
    CONSTRAINT uq_series_author_id UNIQUE (author_id, id)
);

CREATE UNIQUE INDEX uq_series_scope_author_name_lower
    ON series (author_id, institution_id, LOWER(name)) NULLS NOT DISTINCT;

CREATE TABLE category (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    institution_id   BIGINT,
    name             VARCHAR(255) NOT NULL,
    code             VARCHAR(255) NOT NULL,
    CONSTRAINT fk_category_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id)
);

CREATE UNIQUE INDEX uq_category_scope_name_lower
    ON category (organization_id, institution_id, LOWER(name)) NULLS NOT DISTINCT;
CREATE UNIQUE INDEX uq_category_scope_code_lower
    ON category (organization_id, institution_id, LOWER(code)) NULLS NOT DISTINCT;

CREATE TABLE subcategory (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    institution_id   BIGINT,
    name             VARCHAR(255) NOT NULL,
    code             VARCHAR(255) NOT NULL,
    category_id      BIGINT       NOT NULL REFERENCES category (id),
    CONSTRAINT fk_subcategory_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id),
    CONSTRAINT uq_subcategory_category_id UNIQUE (category_id, id)
);

CREATE UNIQUE INDEX uq_subcategory_scope_category_name_lower
    ON subcategory (category_id, institution_id, LOWER(name)) NULLS NOT DISTINCT;
CREATE UNIQUE INDEX uq_subcategory_scope_category_code_lower
    ON subcategory (category_id, institution_id, LOWER(code)) NULLS NOT DISTINCT;

-- Global reference data, shared by every organization. Names of Dewey numbers: a book stores
-- its full notation and takes the name of the most specific entry that exists (863.64 -> 860).
CREATE TABLE dewey_tag (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(255),
    code        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uq_dewey_tag_code UNIQUE (code)
);

-- Malaga author table, global like dewey_tag. Rows are kept as printed, repeated entries and
-- all, so a lookup may suggest several codes. Ch and Ll are letters of their own.
CREATE TABLE malaga_entry (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    letter    VARCHAR(2)   NOT NULL,
    entry     VARCHAR(255) NOT NULL,
    qualifier VARCHAR(255),
    number    INTEGER      NOT NULL
);

CREATE INDEX idx_malaga_entry_letter ON malaga_entry (letter);

CREATE TABLE tag (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    institution_id   BIGINT,
    name             VARCHAR(255) NOT NULL,
    CONSTRAINT fk_tag_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id)
);

CREATE UNIQUE INDEX uq_tag_scope_name_lower
    ON tag (organization_id, institution_id, LOWER(name)) NULLS NOT DISTINCT;

CREATE TABLE book (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    organization_id   BIGINT       NOT NULL REFERENCES organization (id),
    institution_id    BIGINT,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    author_id         BIGINT REFERENCES author (id),
    illustrator_id    BIGINT REFERENCES author (id),
    publisher_id      BIGINT REFERENCES publisher (id),
    collection_id     BIGINT REFERENCES collection (id),
    collection_volume INTEGER,
    series_id         BIGINT REFERENCES series (id),
    series_volume     INTEGER,
    category_id       BIGINT REFERENCES category (id),
    subcategory_id    BIGINT,
    dewey_number      VARCHAR(255),
    country           VARCHAR(255),
    publication_year  INTEGER,
    page_count        INTEGER,
    isbn              VARCHAR(255),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_book_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id),
    CONSTRAINT ck_book_page_count CHECK (page_count IS NULL OR page_count > 0),
    CONSTRAINT ck_collection_requires_publisher
        CHECK (collection_id IS NULL OR publisher_id IS NOT NULL),
    CONSTRAINT fk_collection_publisher
        FOREIGN KEY (publisher_id, collection_id)
        REFERENCES collection (publisher_id, id),
    CONSTRAINT ck_collection_volume_requires_collection
        CHECK (collection_volume IS NULL OR collection_id IS NOT NULL),
    CONSTRAINT ck_collection_volume_positive
        CHECK (collection_volume IS NULL OR collection_volume > 0),
    CONSTRAINT ck_series_requires_author
        CHECK (series_id IS NULL OR author_id IS NOT NULL),
    CONSTRAINT fk_series_author
        FOREIGN KEY (author_id, series_id)
        REFERENCES series (author_id, id),
    CONSTRAINT ck_series_volume_requires_series
        CHECK (series_volume IS NULL OR series_id IS NOT NULL),
    CONSTRAINT ck_series_volume_positive
        CHECK (series_volume IS NULL OR series_volume > 0),
    CONSTRAINT ck_subcategory_requires_category
        CHECK (subcategory_id IS NULL OR category_id IS NOT NULL),
    CONSTRAINT fk_subcategory_category
        FOREIGN KEY (category_id, subcategory_id)
        REFERENCES subcategory (category_id, id),
    CONSTRAINT ck_book_dewey_number
        CHECK (dewey_number IS NULL OR dewey_number ~ '^[0-9]{3}([.][0-9]+)?$')
);

-- COALESCE instead of NULLS NOT DISTINCT so books without an ISBN don't collide
CREATE UNIQUE INDEX uq_book_scope_isbn
    ON book (organization_id, COALESCE(institution_id, 0), isbn);

CREATE INDEX idx_book_institution ON book (institution_id);
CREATE INDEX idx_book_author      ON book (author_id);
CREATE INDEX idx_book_illustrator ON book (illustrator_id);
CREATE INDEX idx_book_publisher   ON book (publisher_id);
CREATE INDEX idx_book_collection  ON book (collection_id);
CREATE INDEX idx_book_series      ON book (series_id);
CREATE INDEX idx_book_category    ON book (category_id);
CREATE INDEX idx_book_subcategory ON book (subcategory_id);
-- Filtering by class or division is a prefix match
CREATE INDEX idx_book_dewey       ON book (dewey_number varchar_pattern_ops);

CREATE TABLE book_collaborator (
    book_id         BIGINT NOT NULL REFERENCES book (id) ON DELETE CASCADE,
    collaborator_id BIGINT NOT NULL REFERENCES author (id),
    PRIMARY KEY (book_id, collaborator_id)
);
CREATE INDEX idx_book_collaborator_author ON book_collaborator (collaborator_id);

CREATE TABLE book_tag (
    book_id BIGINT NOT NULL REFERENCES book (id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tag (id),
    PRIMARY KEY (book_id, tag_id)
);
CREATE INDEX idx_book_tag_tag ON book_tag (tag_id);
