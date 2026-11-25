ALTER TABLE work_day ADD COLUMN before_lunch_value_cents BIGINT;
ALTER TABLE work_day ADD COLUMN after_lunch_value_cents BIGINT;

-- Legacy rows contain no price history. The current company setting is the
-- unavoidable approximation; missing settings fall back to the default zero.
UPDATE work_day SET before_lunch_value_cents = COALESCE(
    (SELECT half_day_value_cents FROM company_settings WHERE company_id = work_day.company_id), 0)
WHERE worked_before_lunch = TRUE;
UPDATE work_day SET after_lunch_value_cents = COALESCE(
    (SELECT half_day_value_cents FROM company_settings WHERE company_id = work_day.company_id), 0)
WHERE worked_after_lunch = TRUE;

ALTER TABLE work_day ADD CONSTRAINT ck_work_day_before_value
    CHECK (before_lunch_value_cents IS NULL OR before_lunch_value_cents >= 0);
ALTER TABLE work_day ADD CONSTRAINT ck_work_day_after_value
    CHECK (after_lunch_value_cents IS NULL OR after_lunch_value_cents >= 0);
ALTER TABLE work_day ADD CONSTRAINT ck_work_day_before_marked_value
    CHECK (NOT worked_before_lunch OR before_lunch_value_cents IS NOT NULL);
ALTER TABLE work_day ADD CONSTRAINT ck_work_day_after_marked_value
    CHECK (NOT worked_after_lunch OR after_lunch_value_cents IS NOT NULL);
