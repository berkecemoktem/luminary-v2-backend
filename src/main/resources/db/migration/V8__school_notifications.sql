-- School-side notifications about invitation responses. Whenever a student
-- accepts or rejects an invite, an entry is created for the inviting
-- institution's notification center so admins know who joined or declined.

CREATE TABLE school_notifications (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            TEXT NOT NULL REFERENCES tenants (id),
    type                 TEXT NOT NULL
                         CHECK (type IN ('INVITE_ACCEPTED', 'INVITE_REJECTED')),
    student_user_id      UUID REFERENCES users (id),
    student_email        TEXT,
    student_display_name TEXT,
    invitation_id        UUID REFERENCES invitations (id),
    read_at              TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_school_notifications_tenant
    ON school_notifications (tenant_id, created_at DESC);

CREATE INDEX idx_school_notifications_unread
    ON school_notifications (tenant_id) WHERE read_at IS NULL;