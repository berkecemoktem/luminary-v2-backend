-- Tenant identifiers become unique, human-friendly string slugs instead of
-- UUIDs (e.g. 'sinav-ankara'). The tables are empty at the time this runs in
-- the demo environment, so the uuid -> text conversion is trivially safe.
-- Child FK columns are rebuilt against the new text primary key.

ALTER TABLE memberships DROP CONSTRAINT memberships_tenant_id_fkey;
ALTER TABLE invitations DROP CONSTRAINT invitations_tenant_id_fkey;

ALTER TABLE tenants ALTER COLUMN id TYPE TEXT;
ALTER TABLE tenants ALTER COLUMN id DROP DEFAULT;

ALTER TABLE memberships ALTER COLUMN tenant_id TYPE TEXT;
ALTER TABLE invitations ALTER COLUMN tenant_id TYPE TEXT;

ALTER TABLE memberships
    ADD CONSTRAINT memberships_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES tenants (id);

ALTER TABLE invitations
    ADD CONSTRAINT invitations_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES tenants (id);