-- V14: recovery codes, for resetting a forgotten password without email.
--
-- Each person can create one recovery code (e.g. K7QF-9XRM-2HPA-W4TD) while logged in,
-- shown to them once to save. "Forgot password" asks for email + recovery code + a new
-- password. Only the code's SHA-256 is stored, so this table can't be used to reset anyone.
-- Using the code replaces it with a new one. After 5 wrong codes the account's reset is
-- locked for an hour.

CREATE TABLE user_recovery_codes (
    user_id         uuid NOT NULL,
    code_hash       varchar(64) NOT NULL,                 -- hex SHA-256 of the code (upper-case, no dashes)
    created_at      timestamp(6) with time zone NOT NULL,
    failed_attempts integer NOT NULL DEFAULT 0,
    locked_until    timestamp(6) with time zone,
    CONSTRAINT user_recovery_codes_pkey PRIMARY KEY (user_id),
    CONSTRAINT fk_recovery_user FOREIGN KEY (user_id) REFERENCES evolyn_user_details (uuid)
);
