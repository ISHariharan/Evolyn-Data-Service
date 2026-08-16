-- V2: body weight, one entry per person per day.
-- Calories are estimated as MET x weight x hours, so every calculation needs
-- the person's weight on (or before) the day of the activity.
-- Weight is private: only its owner ever reads it.

CREATE TABLE user_weight_logs (
    weight_log_id uuid NOT NULL,
    user_id       uuid NOT NULL,
    recorded_on   date NOT NULL,                  -- the day (India time) the weight is for
    weight_kg     numeric(5,2) NOT NULL,
    created_at    timestamp(6) with time zone NOT NULL,
    updated_at    timestamp(6) with time zone NOT NULL,
    CONSTRAINT user_weight_logs_pkey PRIMARY KEY (weight_log_id),
    -- entering a weight twice on the same day corrects that day instead of adding a row
    CONSTRAINT uk_weight_user_day UNIQUE (user_id, recorded_on),
    CONSTRAINT ck_weight_range CHECK (weight_kg BETWEEN 20 AND 300)
);
-- "latest weight on or before a date" is the common lookup; the unique
-- constraint's index (user_id, recorded_on) already serves it.
