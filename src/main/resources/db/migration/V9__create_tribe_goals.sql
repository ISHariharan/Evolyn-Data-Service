-- V9: a tribe's goal for a month, e.g. "60,000 kcal in November".
--
-- Built for more than calories: metric_type and unit say what is counted
-- (only CALORIES_BURNED / kcal for now; distance, minutes or sessions later).
-- Progress is never stored here: it's always added up from the activities,
-- so editing or deleting an activity changes it automatically.

CREATE TABLE tribe_goals (
    goal_id      uuid NOT NULL,
    tribe_id     uuid NOT NULL,
    metric_type  varchar(30) NOT NULL,      -- CALORIES_BURNED
    unit         varchar(10) NOT NULL,      -- kcal
    target_value numeric(12,2) NOT NULL,
    period_type  varchar(10) NOT NULL,      -- MONTHLY
    period_start date NOT NULL,             -- first day of the month (India time)
    period_end   date NOT NULL,             -- last day of the month
    created_by   uuid NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    updated_by   uuid NOT NULL,
    updated_at   timestamp(6) with time zone NOT NULL,
    CONSTRAINT tribe_goals_pkey PRIMARY KEY (goal_id),
    CONSTRAINT fk_goal_tribe FOREIGN KEY (tribe_id) REFERENCES tribes (tribe_id),
    -- one goal per tribe, metric and month
    CONSTRAINT uk_goal_period UNIQUE (tribe_id, metric_type, period_start),
    CONSTRAINT ck_goal_target CHECK (target_value > 0)
);

-- Every change to a target, so "who changed the goal and when" can be answered
CREATE TABLE tribe_goal_changes (
    change_id  uuid NOT NULL,
    goal_id    uuid NOT NULL,
    old_target numeric(12,2),               -- NULL when the goal was first set
    new_target numeric(12,2) NOT NULL,
    changed_by uuid NOT NULL,
    changed_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT tribe_goal_changes_pkey PRIMARY KEY (change_id),
    CONSTRAINT fk_change_goal FOREIGN KEY (goal_id) REFERENCES tribe_goals (goal_id)
);
CREATE INDEX idx_goal_changes_goal ON tribe_goal_changes (goal_id, changed_at);
