-- V10: each tribe's streak rules.
--
-- A person's day counts when they log at least one activity of
-- streak_min_minutes or longer, of a type that counts for goals.
-- The tribe's day counts when at least streak_member_percent of its current
-- members had a counting day.
-- Streaks themselves aren't stored: they're worked out from the activities, so
-- editing or deleting an activity is reflected straight away.

ALTER TABLE tribes
    ADD COLUMN streak_min_minutes integer NOT NULL DEFAULT 20,
    ADD COLUMN streak_member_percent integer NOT NULL DEFAULT 50;

ALTER TABLE tribes
    ADD CONSTRAINT ck_streak_min_minutes CHECK (streak_min_minutes BETWEEN 1 AND 240),
    ADD CONSTRAINT ck_streak_member_percent CHECK (streak_member_percent BETWEEN 1 AND 100);
