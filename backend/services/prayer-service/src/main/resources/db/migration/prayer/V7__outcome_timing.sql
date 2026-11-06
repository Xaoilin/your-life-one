-- Additive: null historical timings retain full legacy completion credit.
ALTER TABLE outcome ADD COLUMN timing text;
ALTER TABLE outcome ADD CONSTRAINT outcome_timing_matches_status CHECK (
    timing IS NULL
    OR (timing IN ('prompt', 'within_window') AND status = 'on_time')
    OR (timing = 'outside_window' AND status = 'late')
);
