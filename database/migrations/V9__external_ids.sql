-- V9: external_id columns for systems and environments
--
-- A separate, URL-safe identifier used by machine/API-key clients to look up
-- a system/environment (see ConfigurationController#revealConfigurationsForEnvironment),
-- decoupled from the human-facing, freely-renameable `name`. Renaming a
-- system/environment never breaks a client already configured with its
-- external id.
--
-- Backfilled from `name` on any existing rows before the NOT NULL constraint
-- is applied, matching the app's own default-to-name behavior on create.

ALTER TABLE systems
    ADD COLUMN external_id VARCHAR(255) NULL AFTER name;

UPDATE systems SET external_id = name WHERE external_id IS NULL;

ALTER TABLE systems
    MODIFY COLUMN external_id VARCHAR(255) NOT NULL,
    ADD UNIQUE KEY uq_systems_external_id (external_id);

ALTER TABLE environments
    ADD COLUMN external_id VARCHAR(255) NULL AFTER name;

UPDATE environments SET external_id = name WHERE external_id IS NULL;

ALTER TABLE environments
    MODIFY COLUMN external_id VARCHAR(255) NOT NULL,
    ADD UNIQUE KEY uq_environments_system_id_external_id (system_id, external_id);

