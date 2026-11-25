-- V13: whether a person shares their weight CHANGE with their tribes.
--
-- Weight stays private by default (see V2). Someone who turns this on lets their
-- tribemates see how their weight has changed in kg over a month (e.g. -1.8 kg),
-- never the weight itself. The percent change isn't shared either: kg change and
-- percent together would give the weight away.

CREATE TABLE user_privacy_settings (
    user_id             uuid NOT NULL,
    share_weight_change boolean NOT NULL DEFAULT false,
    updated_at          timestamp(6) with time zone NOT NULL,
    CONSTRAINT user_privacy_settings_pkey PRIMARY KEY (user_id)
);
