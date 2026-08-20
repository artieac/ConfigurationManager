-- V5: secret_values
--
-- The actual encrypted content, one row per (secret, environment) pair. A
-- secret can be sparse — defined in a system but with no value yet set for
-- some environments — so this is a plain child table (nullable-by-absence),
-- not a required row per environment. Same AES-256-GCM-per-row design as the
-- original single-value `secrets` table this replaced: a fresh random IV on
-- every write, `key_version` for future key rotation.
--
-- No updated_by/updated_at here: secret_value_history (see V6) already records
-- who made every create/update/delete and when, so duplicating "last updated"
-- on this row would just be the same information maintained twice.

CREATE TABLE secret_values (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    secret_id           BIGINT UNSIGNED  NOT NULL,
    environment_id      BIGINT UNSIGNED  NOT NULL,

    encrypted_value     TEXT             NOT NULL,
    encryption_iv       VARCHAR(64)      NOT NULL,
    key_version         INT UNSIGNED     NOT NULL DEFAULT 1,

    created_by          BIGINT UNSIGNED  NOT NULL,
    created_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_secret_values_secret_environment (secret_id, environment_id),
    KEY idx_secret_values_secret_id (secret_id),
    KEY idx_secret_values_environment_id (environment_id),
    CONSTRAINT fk_secret_values_secret      FOREIGN KEY (secret_id)      REFERENCES secrets (id)      ON DELETE CASCADE,
    CONSTRAINT fk_secret_values_environment FOREIGN KEY (environment_id) REFERENCES environments (id) ON DELETE CASCADE,
    CONSTRAINT fk_secret_values_created_by  FOREIGN KEY (created_by)     REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
