-- Adds a rejection decision to invitations so a signed-in invitee can decline
-- a suspicious or irrelevant invite, and so an accepted invite invalidates all
-- other pending invites addressed to the same email (one-accept rule).

ALTER TABLE invitations ADD COLUMN rejected_at TIMESTAMPTZ;