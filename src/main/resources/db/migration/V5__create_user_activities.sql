-- V5: activities people log, and the calorie calculation for each.
--
-- An activity belongs to a person (not to a tribe), so one workout counts
-- everywhere it qualifies.
--
-- Calculations are kept in their own table with every input they used
-- (MET, weight, duration, formula, Compendium edition), so a number can always
-- be explained later. Editing an activity adds a new calculation and marks the
-- old one as no longer current; nothing is overwritten.

CREATE TABLE user_activities (
    activity_id            uuid NOT NULL,
    user_id                uuid NOT NULL,
    activity_definition_id bigint NOT NULL,
    started_at             timestamp(6) with time zone NOT NULL,
    activity_date          date NOT NULL,              -- the day (India time) it started
    duration_seconds       integer NOT NULL,
    distance_km            numeric(7,2),               -- optional: running, cycling, walking...
    notes                  varchar(500),
    source                 varchar(20) NOT NULL,       -- MANUAL (device imports later)
    visibility             varchar(20) NOT NULL,       -- TRIBES or PRIVATE
    created_at             timestamp(6) with time zone NOT NULL,
    updated_at             timestamp(6) with time zone NOT NULL,
    deleted_at             timestamp(6) with time zone, -- deleted activities are kept, just hidden
    CONSTRAINT user_activities_pkey PRIMARY KEY (activity_id),
    CONSTRAINT fk_user_activity_definition FOREIGN KEY (activity_definition_id)
        REFERENCES activity_definitions (activity_definition_id),
    CONSTRAINT ck_activity_duration CHECK (duration_seconds > 0 AND duration_seconds <= 86400),
    CONSTRAINT ck_activity_distance CHECK (distance_km IS NULL OR distance_km >= 0)
);
CREATE INDEX idx_user_activities_user_date ON user_activities (user_id, activity_date);

CREATE TABLE activity_calculations (
    calculation_id          uuid NOT NULL,
    activity_id             uuid NOT NULL,
    metric_type             varchar(30) NOT NULL,      -- CALORIES_BURNED (others later)
    metric_value            numeric(10,2) NOT NULL,    -- kcal, kept with 2 decimals; rounded for display
    unit                    varchar(10) NOT NULL,      -- kcal
    formula_version         varchar(30) NOT NULL,      -- MET_GROSS_V1 = MET x kg x hours
    met_value               numeric(4,1) NOT NULL,
    weight_kg_used          numeric(5,2) NOT NULL,
    weight_recorded_on      date NOT NULL,             -- which day's weight was used
    duration_seconds        integer NOT NULL,
    reference_edition       varchar(20) NOT NULL,      -- e.g. 2024_ADULT
    reference_activity_code varchar(5) NOT NULL,       -- e.g. 15330
    is_current              boolean NOT NULL,
    calculated_at           timestamp(6) with time zone NOT NULL,
    CONSTRAINT activity_calculations_pkey PRIMARY KEY (calculation_id),
    CONSTRAINT fk_calculation_activity FOREIGN KEY (activity_id) REFERENCES user_activities (activity_id)
);
-- At most one current calculation per activity and metric
CREATE UNIQUE INDEX uk_calculation_current ON activity_calculations (activity_id, metric_type) WHERE is_current;
