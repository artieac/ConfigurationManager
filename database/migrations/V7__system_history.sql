-- V7: system_history
--
-- Append-only audit trail for systems, mirroring secret_value_history (V6): every
-- create/rename/delete gets its own row rather than being folded into
-- updated_by/updated_at on `systems` itself (see V2). No encrypted-value
-- snapshot columns here — a system carries no secret content of its own.
--
-- `system_id` is nullable with ON DELETE SET NULL, and `system_name` is a
-- denormalized snapshot, so deleting a system (ADMIN-only) can never erase
-- its audit trail.

CREATE TABLE system_history (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,

    system_id           BIGINT UNSIGNED  NULL,
    system_name         VARCHAR(255)     NOT NULL,

    action              ENUM('CREATED', 'UPDATED', 'DELETED') NOT NULL,

    changed_by          BIGINT UNSIGNED  NOT NULL,
    changed_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    KEY idx_system_history_system_id (system_id),
    KEY idx_system_history_changed_at (changed_at),
    CONSTRAINT fk_system_history_system     FOREIGN KEY (system_id)  REFERENCES systems (id) ON DELETE SET NULL,
    CONSTRAINT fk_system_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
