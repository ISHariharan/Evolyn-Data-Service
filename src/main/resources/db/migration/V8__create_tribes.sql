-- V8: tribes - groups of friends who work out together.
--
-- For now every member can do everything (edit, invite, remove, archive).
-- created_by and invited_by are kept so roles can be added later without
-- another table change.

CREATE TABLE tribes (
    tribe_id             uuid NOT NULL,
    name                 varchar(60) NOT NULL,
    description          varchar(300),
    created_by           uuid NOT NULL,
    created_at           timestamp(6) with time zone NOT NULL,
    updated_at           timestamp(6) with time zone NOT NULL,
    archived_at          timestamp(6) with time zone,     -- archived tribes are hidden and read-only
    join_code            varchar(12),                     -- invite link code, e.g. HB7Q4KM2 (NULL = link off)
    join_code_expires_at timestamp(6) with time zone,
    CONSTRAINT tribes_pkey PRIMARY KEY (tribe_id),
    CONSTRAINT uk_tribes_join_code UNIQUE (join_code)
);

-- Who is (or was) in a tribe. A member who leaves keeps their row (status LEFT)
-- so history stays explainable; joining again makes the same row ACTIVE again.
CREATE TABLE tribe_members (
    member_id  uuid NOT NULL,
    tribe_id   uuid NOT NULL,
    user_id    uuid NOT NULL,
    status     varchar(10) NOT NULL,                      -- ACTIVE, LEFT, REMOVED
    joined_at  timestamp(6) with time zone NOT NULL,
    left_at    timestamp(6) with time zone,
    removed_by uuid,
    CONSTRAINT tribe_members_pkey PRIMARY KEY (member_id),
    CONSTRAINT uk_tribe_member UNIQUE (tribe_id, user_id),
    CONSTRAINT fk_member_tribe FOREIGN KEY (tribe_id) REFERENCES tribes (tribe_id),
    CONSTRAINT ck_member_status CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED'))
);
CREATE INDEX idx_tribe_members_user ON tribe_members (user_id, status);

-- Invitations by Profile ID. The invited person must accept before joining.
CREATE TABLE tribe_invitations (
    invitation_id   uuid NOT NULL,
    tribe_id        uuid NOT NULL,
    invited_user_id uuid NOT NULL,
    invited_by      uuid NOT NULL,
    status          varchar(10) NOT NULL,                 -- PENDING, ACCEPTED, DECLINED, REVOKED
    created_at      timestamp(6) with time zone NOT NULL,
    expires_at      timestamp(6) with time zone NOT NULL, -- PENDING past this = expired
    responded_at    timestamp(6) with time zone,
    CONSTRAINT tribe_invitations_pkey PRIMARY KEY (invitation_id),
    CONSTRAINT fk_invitation_tribe FOREIGN KEY (tribe_id) REFERENCES tribes (tribe_id),
    CONSTRAINT ck_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED'))
);
CREATE INDEX idx_invitations_invitee ON tribe_invitations (invited_user_id, status);
CREATE INDEX idx_invitations_tribe ON tribe_invitations (tribe_id, status);
-- At most one open invitation per person per tribe
CREATE UNIQUE INDEX uk_invitation_pending ON tribe_invitations (tribe_id, invited_user_id) WHERE status = 'PENDING';
