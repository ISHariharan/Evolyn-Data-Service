-- V12: each person's daily calorie goal, e.g. "burn 400 kcal a day".
--
-- A tribe's monthly goal is no longer typed in: it's the sum of its members'
-- daily goals over the month's days (see Goals/DailyGoalMath).
--
-- A change takes effect from the day it's made (India date) and never rewrites
-- earlier days, so each change is a new row. Changing it twice on the same day
-- just corrects that day's row.

CREATE TABLE user_daily_goals (
    user_id        uuid NOT NULL,
    effective_from date NOT NULL,
    kcal           integer NOT NULL,
    set_at         timestamp(6) with time zone NOT NULL,
    CONSTRAINT user_daily_goals_pkey PRIMARY KEY (user_id, effective_from),
    CONSTRAINT ck_daily_goal_kcal CHECK (kcal BETWEEN 50 AND 5000)
);

-- tribe_goals / tribe_goal_changes (V9) are kept for history but no longer written.
