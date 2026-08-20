-- V4: environments
--
-- A system can define any number of named environments (e.g. "Development",
-- "Staging", "Production") to hold environment-specific secret values against.
-- Environments are scoped to a system (not global) — "Production" in one
-- system is a distinct row from "Production" in another.

CREATE TABLE environments (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    system_id           BIGINT UNSIGNED  NOT NULL,
    name                VARCHAR(100)     NOT NULL,

    created_by          BIGINT UNSIGNED  NOT NULL,
    created_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by          BIGINT UNSIGNED  NOT NULL,
    updated_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_environments_system_name (system_id, name),
    KEY idx_environments_system_id (system_id),
    CONSTRAINT fk_environments_system     FOREIGN KEY (system_id)  REFERENCES systems (id) ON DELETE CASCADE,
    CONSTRAINT fk_environments_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_environments_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
