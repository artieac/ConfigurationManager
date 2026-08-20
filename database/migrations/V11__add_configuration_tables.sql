-- V11: add configuration tables and migrate data
--
-- Creates the new configuration tables (configurations, configuration_values, configuration_value_history)
-- to replace the old secret tables. Copies the existing data over. The old tables
-- are left intact for rollback purposes.

CREATE TABLE configurations (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    system_id           BIGINT UNSIGNED  NOT NULL,
    name                VARCHAR(255)     NOT NULL,

    created_by          BIGINT UNSIGNED  NOT NULL,
    created_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by          BIGINT UNSIGNED  NOT NULL,
    updated_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_configurations_system_name (system_id, name),
    KEY idx_configurations_system_id (system_id),
    CONSTRAINT fk_configurations_system     FOREIGN KEY (system_id)  REFERENCES systems (id) ON DELETE CASCADE,
    CONSTRAINT fk_configurations_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_configurations_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE configuration_values (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    configuration_id    BIGINT UNSIGNED  NOT NULL,
    environment_id      BIGINT UNSIGNED  NOT NULL,

    encrypted_value     TEXT             NOT NULL,
    encryption_iv       VARCHAR(64)      NOT NULL,
    key_version         INT UNSIGNED     NOT NULL DEFAULT 1,

    created_by          BIGINT UNSIGNED  NOT NULL,
    created_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_config_values_config_env (configuration_id, environment_id),
    KEY idx_config_values_config_id (configuration_id),
    KEY idx_config_values_env_id (environment_id),
    CONSTRAINT fk_config_values_config      FOREIGN KEY (configuration_id) REFERENCES configurations (id) ON DELETE CASCADE,
    CONSTRAINT fk_config_values_environment FOREIGN KEY (environment_id)   REFERENCES environments (id) ON DELETE CASCADE,
    CONSTRAINT fk_config_values_created_by  FOREIGN KEY (created_by)       REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE configuration_value_history (
    id                          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,

    configuration_id            BIGINT UNSIGNED  NULL,
    system_id                   BIGINT UNSIGNED  NULL,
    environment_id              BIGINT UNSIGNED  NULL,
    configuration_name          VARCHAR(255)     NOT NULL,
    system_name                 VARCHAR(255)     NOT NULL,
    environment_name            VARCHAR(100)     NOT NULL,

    action                      ENUM('CREATED', 'UPDATED', 'DELETED') NOT NULL,

    encrypted_value_snapshot    TEXT             NULL,
    encryption_iv_snapshot      VARCHAR(64)      NULL,
    key_version                 INT UNSIGNED     NULL,

    changed_by                  BIGINT UNSIGNED  NOT NULL,
    changed_at                  DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY idx_config_value_history_config_id (configuration_id),
    KEY idx_config_value_history_system_id (system_id),
    KEY idx_config_value_history_env_id (environment_id),
    KEY idx_config_value_history_changed_at (changed_at),
    CONSTRAINT fk_config_value_history_config      FOREIGN KEY (configuration_id) REFERENCES configurations (id) ON DELETE SET NULL,
    CONSTRAINT fk_config_value_history_system      FOREIGN KEY (system_id)        REFERENCES systems (id)        ON DELETE SET NULL,
    CONSTRAINT fk_config_value_history_environment FOREIGN KEY (environment_id)   REFERENCES environments (id)   ON DELETE SET NULL,
    CONSTRAINT fk_config_value_history_changed_by  FOREIGN KEY (changed_by)       REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Migrate data
INSERT INTO configurations (id, system_id, name, created_by, created_at, updated_by, updated_at)
SELECT id, system_id, name, created_by, created_at, updated_by, updated_at FROM secrets;

INSERT INTO configuration_values (id, configuration_id, environment_id, encrypted_value, encryption_iv, key_version, created_by, created_at)
SELECT id, secret_id, environment_id, encrypted_value, encryption_iv, key_version, created_by, created_at FROM secret_values;

INSERT INTO configuration_value_history (id, configuration_id, system_id, environment_id, configuration_name, system_name, environment_name, action, encrypted_value_snapshot, encryption_iv_snapshot, key_version, changed_by, changed_at)
SELECT id, secret_id, system_id, environment_id, secret_name, system_name, environment_name, action, encrypted_value_snapshot, encryption_iv_snapshot, key_version, changed_by, changed_at FROM secret_value_history;
