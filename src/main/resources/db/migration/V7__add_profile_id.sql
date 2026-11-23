-- V7: Profile ID, a public handle like "arun_k" that friends use to find and
-- invite each other without sharing emails.
--
-- Existing accounts start without one (NULL); the app asks them to pick one at
-- their next login. New accounts choose one when they sign up.
-- Always stored in lowercase, so "Arun_K" and "arun_k" can't both exist.

ALTER TABLE evolyn_user_details
    ADD COLUMN profile_id varchar(20);

ALTER TABLE evolyn_user_details
    ADD CONSTRAINT ck_profile_id_format CHECK (profile_id ~ '^[a-z0-9_]{3,20}$');

-- Unique among the accounts that have one (several NULLs are allowed)
CREATE UNIQUE INDEX uk_user_profile_id ON evolyn_user_details (profile_id);
