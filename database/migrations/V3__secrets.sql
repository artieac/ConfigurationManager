-- V3: secrets
--
-- A secret is a NAME scoped to a system — it no longer carries a value itself.
-- The value lives per-environment in `secret_values` (see V5), so the same
-- secret name (e.g. "DB_PASSWORD") can hold a different value in each of a
-- system's environments (Development/Staging/Production/...) while still
-- being recognizably "the same secret" across them.

CREATE TABLE secrets (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    system_id           BIGINT UNSIGNED  NOT NULL,
    name                VARCHAR(255)     NOT NULL,

    created_by          BIGINT UNSIGNED  NOT NULL,
    created_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by          BIGINT UNSIGNED  NOT NULL,
    updated_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_secrets_system_name (system_id, name),
    KEY idx_secrets_system_id (system_id),
    CONSTRAINT fk_secrets_system     FOREIGN KEY (system_id)  REFERENCES systems (id) ON DELETE CASCADE,
    CONSTRAINT fk_secrets_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_secrets_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
