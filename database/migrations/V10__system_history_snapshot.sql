-- V10: system_history snapshot columns
--
-- system_history originally only snapshotted `system_name`, so the audit
-- trail could say a system was renamed/updated but not show what actually
-- changed. Adds external_id/description snapshots alongside it so each row
-- captures the system's full state at that point in time — the UI can then
-- diff a row against the one before it to show the actual before/after.
--
-- Nullable, with no backfill: rows written before this migration genuinely
-- never captured external_id/description, so there's nothing honest to fill
-- them in with. Every row written going forward will always populate both.

ALTER TABLE system_history
    ADD COLUMN external_id VARCHAR(255) NULL AFTER system_name,
    ADD COLUMN description VARCHAR(1000) NULL AFTER external_id;
