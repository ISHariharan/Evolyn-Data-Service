-- V6: which Compendium headings count towards tribe goals, challenges and streaks.
--
-- Every activity can still be logged (and shows its estimated calories), but
-- only these headings add to shared goals. Without this, 8 hours of sleep
-- (Inactivity, ~1 MET) would put ~550 kcal on the leaderboard.

ALTER TABLE compendium_headings
    ADD COLUMN counts_for_goals boolean NOT NULL DEFAULT false;

UPDATE compendium_headings SET counts_for_goals = true
WHERE heading_code IN (
    '01',  -- Bicycling
    '02',  -- Conditioning Exercise (gym, aerobics, circuits)
    '03',  -- Dancing
    '05',  -- Home Activities
    '08',  -- Lawn & Garden
    '12',  -- Running
    '15',  -- Sports (team handball is 15330)
    '17',  -- Walking
    '18',  -- Water Activities (swimming)
    '19'   -- Winter Activities
);
-- Not counted: 04 Fishing & Hunting, 06 Home Repair, 07 Inactivity, 09 Miscellaneous,
-- 10 Music Playing, 11 Occupation, 13 Self Care, 14 Sexual Activity, 16 Transportation,
-- 20 Religious Activities, 21 Volunteer Activities, 22 Video Games.
