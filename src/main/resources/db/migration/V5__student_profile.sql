-- Add geographic fields to the global user identity.
ALTER TABLE users ADD COLUMN country TEXT;
ALTER TABLE users ADD COLUMN city    TEXT;

-- Tenant-scoped student profile: grade, academic field, and school.
-- One profile per user per tenant; unclaimed profiles (invite-only
-- registration) are possible if user_id is set at accept time.
CREATE TABLE student_profiles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     TEXT        NOT NULL REFERENCES tenants (id),
    user_id       UUID        NOT NULL REFERENCES users (id),
    grade_level   TEXT        NOT NULL
                  CHECK (grade_level IN ('GRADE_11', 'GRADE_12', 'GRADUATE')),
    field         TEXT        NOT NULL
                  CHECK (field IN ('SAYISAL', 'ESIT_AGIRLIK', 'SOZEL', 'DIL')),
    school_name   TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_profile_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_student_profiles_user   ON student_profiles (user_id);
CREATE INDEX idx_student_profiles_tenant ON student_profiles (tenant_id);
