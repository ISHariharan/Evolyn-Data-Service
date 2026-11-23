-- V11: challenges - time-boxed contests inside a tribe, separate from the monthly goal.
--
-- Five types share one table:
--   COMPETITIVE        highest total kcal wins
--   COLLECTIVE         everyone adds to one shared kcal target
--   INDIVIDUAL_TARGET  each participant aims for the same kcal target
--   CONSISTENCY        number of different days with a counting activity (target = days)
--   TEAM               participants pick team A or B; team totals decide
--
-- Standings are worked out from the activities while a challenge runs. A day
-- after it ends ("under review": late logging of in-period activities is still
-- possible) the results are frozen into challenge_results and never change.
-- Any tribe member can create, publish or cancel (no roles yet).

CREATE TABLE challenges (
    challenge_id   uuid NOT NULL,
    tribe_id       uuid NOT NULL,
    created_by     uuid NOT NULL,
    name           varchar(80) NOT NULL,
    description    varchar(500),
    challenge_type varchar(20) NOT NULL,
    metric_type    varchar(30) NOT NULL,       -- CALORIES_BURNED or ACTIVE_DAYS (consistency)
    target_value   numeric(12,2),              -- kcal, or days for CONSISTENCY; NULL for COMPETITIVE/TEAM
    start_date     date NOT NULL,              -- India dates, both included
    end_date       date NOT NULL,
    min_minutes    integer NOT NULL DEFAULT 20,
    heading_code   varchar(2),                 -- only this activity heading counts (NULL = any that counts for goals)
    status         varchar(12) NOT NULL,       -- DRAFT, PUBLISHED, CANCELLED (UPCOMING/ACTIVE/... come from the dates)
    published_at   timestamp(6) with time zone,
    cancelled_at   timestamp(6) with time zone,
    finalized_at   timestamp(6) with time zone, -- results frozen
    created_at     timestamp(6) with time zone NOT NULL,
    updated_at     timestamp(6) with time zone NOT NULL,
    CONSTRAINT challenges_pkey PRIMARY KEY (challenge_id),
    CONSTRAINT fk_challenge_tribe FOREIGN KEY (tribe_id) REFERENCES tribes (tribe_id),
    CONSTRAINT fk_challenge_heading FOREIGN KEY (heading_code) REFERENCES compendium_headings (heading_code),
    CONSTRAINT ck_challenge_type CHECK (challenge_type IN ('COMPETITIVE', 'COLLECTIVE', 'INDIVIDUAL_TARGET', 'CONSISTENCY', 'TEAM')),
    CONSTRAINT ck_challenge_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    CONSTRAINT ck_challenge_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_challenge_min CHECK (min_minutes BETWEEN 1 AND 240),
    CONSTRAINT ck_challenge_target CHECK (target_value IS NULL OR target_value > 0)
);
CREATE INDEX idx_challenges_tribe ON challenges (tribe_id, start_date);

CREATE TABLE challenge_participants (
    participant_id uuid NOT NULL,
    challenge_id   uuid NOT NULL,
    user_id        uuid NOT NULL,
    team           varchar(1),                 -- A or B for TEAM challenges
    status         varchar(10) NOT NULL,       -- JOINED, LEFT
    joined_at      timestamp(6) with time zone NOT NULL,
    left_at        timestamp(6) with time zone,
    CONSTRAINT challenge_participants_pkey PRIMARY KEY (participant_id),
    CONSTRAINT uk_challenge_participant UNIQUE (challenge_id, user_id),
    CONSTRAINT fk_participant_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (challenge_id),
    CONSTRAINT ck_participant_team CHECK (team IS NULL OR team IN ('A', 'B')),
    CONSTRAINT ck_participant_status CHECK (status IN ('JOINED', 'LEFT'))
);

-- Frozen final standings: one row per person (and per team for TEAM challenges)
CREATE TABLE challenge_results (
    result_id     uuid NOT NULL,
    challenge_id  uuid NOT NULL,
    subject_type  varchar(6) NOT NULL,         -- USER or TEAM
    user_id       uuid,
    team          varchar(1),
    value         numeric(12,2) NOT NULL,      -- kcal, or days for CONSISTENCY
    result_rank   integer,
    completed     boolean NOT NULL,            -- reached the target / won
    finalized_at  timestamp(6) with time zone NOT NULL,
    CONSTRAINT challenge_results_pkey PRIMARY KEY (result_id),
    CONSTRAINT fk_result_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (challenge_id),
    CONSTRAINT ck_result_subject CHECK (subject_type IN ('USER', 'TEAM'))
);
CREATE INDEX idx_challenge_results ON challenge_results (challenge_id);
