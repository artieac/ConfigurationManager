-- V6: secret_value_history
--
-- Append-only audit trail for changes to a secret's per-environment value
-- (see V5 secret_values). Kept as a separate table from `secret_values` so
-- the hot path — reading current values — never scans historical rows.
--
-- `secret_id` / `system_id` / `environment_id` are all nullable with
-- ON DELETE SET NULL: an ADMIN deleting a secret, an environment, or an
-- entire system must not be able to erase the audit trail of what happened
-- before the deletion. The `*_name` columns are denormalized snapshots so
-- history rows stay readable even after the parent row (and its name) is gone.
--
-- `encrypted_value_snapshot`/`encryption_iv_snapshot` retain the ciphertext
-- (never plaintext) as of that change, for forensic purposes only. No current
-- API endpoint decrypts or returns these columns — history is exposed to
-- clients as metadata (who/when/what/which environment) only.

CREATE TABLE secret_value_history (
    id                          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,

    secret_id                   BIGINT UNSIGNED  NULL,
    system_id                   BIGINT UNSIGNED  NULL,
    environment_id              BIGINT UNSIGNED  NULL,
    secret_name                 VARCHAR(255)     NOT NULL,
    system_name                 VARCHAR(255)     NOT NULL,
    environment_name            VARCHAR(100)     NOT NULL,

    action                      ENUM('CREATED', 'UPDATED', 'DELETED') NOT NULL,

    encrypted_value_snapshot    TEXT             NULL,
    encryption_iv_snapshot      VARCHAR(64)      NULL,
    key_version                 INT UNSIGNED     NULL,

    changed_by                  BIGINT UNSIGNED  NOT NULL,
    changed_at                  DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY idx_secret_value_history_secret_id (secret_id),
    KEY idx_secret_value_history_system_id (system_id),
    KEY idx_secret_value_history_environment_id (environment_id),
    KEY idx_secret_value_history_changed_at (changed_at),
    CONSTRAINT fk_secret_value_history_secret      FOREIGN KEY (secret_id)      REFERENCES secrets (id)      ON DELETE SET NULL,
    CONSTRAINT fk_secret_value_history_system      FOREIGN KEY (system_id)      REFERENCES systems (id)      ON DELETE SET NULL,
    CONSTRAINT fk_secret_value_history_environment FOREIGN KEY (environment_id) REFERENCES environments (id) ON DELETE SET NULL,
    CONSTRAINT fk_secret_value_history_changed_by  FOREIGN KEY (changed_by)     REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
