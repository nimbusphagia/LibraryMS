-- V1: catalog module schema
-- Every table mirrors BaseEntity: id, created_at, updated_at, version.

CREATE TABLE author (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255)
);

CREATE UNIQUE INDEX uq_author_full_name
    ON author (LOWER(first_name), LOWER(last_name)) NULLS NOT DISTINCT;

CREATE TABLE publisher (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uq_publisher_name_lower ON publisher (LOWER(name));

CREATE TABLE collection (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0,
    name         VARCHAR(255) NOT NULL,
    publisher_id BIGINT       NOT NULL REFERENCES publisher (id),
    CONSTRAINT uq_collection_publisher_id UNIQUE (publisher_id, id)
);

CREATE UNIQUE INDEX uq_collection_publisher_name_lower
    ON collection (publisher_id, LOWER(name));

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

CREATE TABLE tag (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uq_tag_name_lower ON tag (LOWER(name));

CREATE TABLE book (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    author_id        BIGINT REFERENCES author (id),
    illustrator_id   BIGINT REFERENCES author (id),
    publisher_id     BIGINT REFERENCES publisher (id),
    collection_id    BIGINT REFERENCES collection (id),
    dewey_id         BIGINT REFERENCES dewey_tag (id),
    country          VARCHAR(255),
    publication_year INTEGER,
    page_count       INTEGER,
    isbn             VARCHAR(255),
    CONSTRAINT uq_book_isbn UNIQUE (isbn),
    CONSTRAINT ck_book_page_count CHECK (page_count IS NULL OR page_count > 0),
    CONSTRAINT ck_collection_requires_publisher
        CHECK (collection_id IS NULL OR publisher_id IS NOT NULL),
    CONSTRAINT fk_collection_publisher
        FOREIGN KEY (publisher_id, collection_id)
        REFERENCES collection (publisher_id, id)
);

CREATE INDEX idx_book_author      ON book (author_id);
CREATE INDEX idx_book_illustrator ON book (illustrator_id);
CREATE INDEX idx_book_publisher   ON book (publisher_id);
CREATE INDEX idx_collection  ON book (collection_id);
CREATE INDEX idx_book_dewey       ON book (dewey_id);

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
