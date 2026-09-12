CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_issuer   TEXT        NOT NULL,
    auth_subject  TEXT        NOT NULL,
    email         TEXT,
    display_name  TEXT        NOT NULL DEFAULT '',
    status        TEXT        NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_auth_identity UNIQUE (auth_issuer, auth_subject)
);

CREATE TABLE tenants (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type      TEXT        NOT NULL CHECK (type IN ('INSTITUTION', 'PERSONAL')),
    name      TEXT        NOT NULL,
    status    TEXT        NOT NULL DEFAULT 'ACTIVE'
              CHECK (status IN ('ACTIVE', 'DISABLED')),
    time_zone TEXT        NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE memberships (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL REFERENCES tenants (id),
    user_id    UUID        NOT NULL REFERENCES users (id),
    role       TEXT        NOT NULL CHECK (role IN ('STUDENT', 'INSTITUTION_ADMIN')),
    status     TEXT        NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE', 'REVOKED')),
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uq_membership_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_memberships_user ON memberships (user_id);
CREATE INDEX idx_memberships_tenant ON memberships (tenant_id, status);

CREATE TABLE invitations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID        NOT NULL REFERENCES tenants (id),
    email             TEXT        NOT NULL,
    role              TEXT        NOT NULL CHECK (role IN ('STUDENT', 'INSTITUTION_ADMIN')),
    token_hash        TEXT        NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,
    used_at           TIMESTAMPTZ,
    created_by_user_id UUID       REFERENCES users (id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invitations_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_invitations_tenant ON invitations (tenant_id);
CREATE INDEX idx_invitations_email ON invitations (email);