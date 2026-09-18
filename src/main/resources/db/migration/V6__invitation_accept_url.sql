-- Persists the full single-use accept URL so a signed-in invitee can later
-- reopen the invitation from the in-app notification center. Invitations
-- created before this migration keep a NULL accept_url and are not listed
-- as pending in-app unless a fresh one is issued.

ALTER TABLE invitations ADD COLUMN accept_url TEXT;