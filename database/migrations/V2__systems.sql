-- V2: systems
--
-- A "system" is the container a user creates to group related secrets
-- (e.g. "Payments API", "Customer Portal"). created_by is an FK to users
-- rather than free-text so history joins stay referentially sound.
--
-- No updated_by/updated_at here: system_history (see V7) records every
-- create/rename/delete with who and when, so duplicating "last updated" on
-- this row would just be the same information maintained twice.

CREATE TABLE systems (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255)     NOT NULL,
    description     VARCHAR(1000)    NULL,

    created_by      BIGINT UNSIGNED  NOT NULL,
    created_at      DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_systems_name (name),
    CONSTRAINT fk_systems_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
