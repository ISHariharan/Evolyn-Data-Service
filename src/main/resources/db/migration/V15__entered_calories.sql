-- V15: people can enter the calories they burned (from a watch or a gym machine)
-- instead of using the estimate.
--
-- An entered number is saved as a calculation with formula_version 'ENTERED_V1':
-- metric_value is what they typed, and the MET and weight (when there is one) are
-- still saved alongside, so it can be compared with the estimate later.
-- Entering calories doesn't need a saved weight, so the weight columns may be empty.

ALTER TABLE activity_calculations ALTER COLUMN weight_kg_used DROP NOT NULL;
ALTER TABLE activity_calculations ALTER COLUMN weight_recorded_on DROP NOT NULL;
