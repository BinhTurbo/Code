CREATE TABLE roles (
                       id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                       code         VARCHAR(50)  NOT NULL UNIQUE,
                       name         VARCHAR(100) NOT NULL,
                       created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
                       id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username       VARCHAR(100) NOT NULL UNIQUE,
                       password_hash  VARCHAR(255) NOT NULL,
                       full_name      VARCHAR(150),
                       role_id        BIGINT       NOT NULL,
                       status         ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
                       created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT fk_users_role FOREIGN KEY (role_id)
                           REFERENCES roles(id)
                           ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_status  ON users(status);
