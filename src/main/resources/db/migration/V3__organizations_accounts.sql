-- V3: organizations, institutions & accounts

-- An organization is one owner's isolated network of institutions.
CREATE TABLE organization (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(255) NOT NULL
);

-- An institution (e.g. a school) groups libraries that share staff and visitors.
CREATE TABLE institution (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id),
    name             VARCHAR(255) NOT NULL,
    time_zone        VARCHAR(64)  NOT NULL DEFAULT 'America/Lima',
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_institution_organization_id UNIQUE (organization_id, id)
);

CREATE UNIQUE INDEX uq_institution_organization_name_lower
    ON institution (organization_id, LOWER(name));

CREATE TABLE account (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version               BIGINT       NOT NULL DEFAULT 0,
    organization_id       BIGINT       NOT NULL REFERENCES organization (id),
    institution_id        BIGINT,
    username              VARCHAR(100) NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    role                  VARCHAR(50)  NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_account_role CHECK (role IN ('OWNER', 'ADMIN', 'LIBRARIAN', 'STAFF')),
    -- Owners and admins work across the organization; client roles belong to one institution.
    CONSTRAINT ck_account_institution_by_role CHECK (
        (role IN ('OWNER', 'ADMIN') AND institution_id IS NULL)
     OR (role IN ('LIBRARIAN', 'STAFF') AND institution_id IS NOT NULL)),
    CONSTRAINT fk_account_institution_organization
        FOREIGN KEY (organization_id, institution_id)
        REFERENCES institution (organization_id, id)
);

CREATE UNIQUE INDEX uq_account_username_lower ON account (LOWER(username));
CREATE UNIQUE INDEX uq_account_single_owner ON account (organization_id) WHERE role = 'OWNER';
CREATE INDEX idx_account_institution ON account (institution_id);

CREATE TABLE spring_session (
    primary_id            CHAR(36) NOT NULL,
    session_id            CHAR(36) NOT NULL,
    creation_time         BIGINT   NOT NULL,
    last_access_time      BIGINT   NOT NULL,
    max_inactive_interval INT      NOT NULL,
    expiry_time           BIGINT   NOT NULL,
    principal_name        VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36)     NOT NULL,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
