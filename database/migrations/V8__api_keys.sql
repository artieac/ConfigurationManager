-- V8: api_keys
--
-- Machine-to-machine credentials, scoped to exactly one system, for pulling
-- that system's secrets without a browser session (e.g. a CI/CD deploy job
-- calling GET /api/systems/{id}/environments/{id}/secrets). Each client gets
-- its own key so access can be granted/revoked per client rather than
-- sharing one secret across every consumer.
--
-- Only a SHA-256 hash of the token is ever stored — the raw token is shown
-- to the caller exactly once, at creation, and is not recoverable after
-- that (the same "can't get it back, only reissue" pattern as GitHub/Stripe/
-- AWS API keys). `token_hash` is unique so a hash collision can never let
-- one key masquerade as another.

CREATE TABLE api_keys (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    system_id       BIGINT UNSIGNED  NOT NULL,
    name            VARCHAR(255)     NOT NULL,
    token_hash      VARCHAR(64)      NOT NULL,

    created_by      BIGINT UNSIGNED  NOT NULL,
    created_at      DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_used_at    DATETIME(6)      NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_api_keys_token_hash (token_hash),
    KEY idx_api_keys_system_id (system_id),
    CONSTRAINT fk_api_keys_system     FOREIGN KEY (system_id)  REFERENCES systems (id) ON DELETE CASCADE,
    CONSTRAINT fk_api_keys_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
