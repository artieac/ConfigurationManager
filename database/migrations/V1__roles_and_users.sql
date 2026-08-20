CREATE DATABASE IF NOT EXISTS ConfigurationManager
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE ConfigurationManager;



-- V1: roles and users
--
-- Roles are a fixed, small lookup table rather than a free-text column so that
-- FK constraints keep `users.role_id` valid and role names can be renamed/extended
-- later without touching every row.

CREATE TABLE roles (
    id              INT  			 NOT NULL AUTO_INCREMENT,
    name            VARCHAR(32)      NOT NULL,
    description     VARCHAR(255)     NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO roles (name, description) VALUES
    ('READ_ONLY',  'Can view systems, secret names, and secret history. Can never view decrypted secret values.'),
    ('READ_WRITE', 'Everything READ_ONLY can do, plus create/update systems and secrets, and reveal decrypted secret values.'),
    ('ADMIN',      'Everything READ_WRITE can do, plus delete systems/secrets and manage user role assignments.');

-- One row per person who has ever logged in via Auth0. Rows are created/updated
-- by the backend on login (see AuthService) — there is no self-registration path.
CREATE TABLE users (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    auth0_user_id   VARCHAR(255)     NOT NULL,
    email           VARCHAR(320)     NOT NULL,
    display_name    VARCHAR(255)     NULL,
    role_id         INT  			 NOT NULL,
    is_active       TINYINT(1)       NOT NULL DEFAULT 1,
    created_at      DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_login_at   DATETIME(6)      NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_auth0_user_id (auth0_user_id),
    KEY idx_users_email (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

