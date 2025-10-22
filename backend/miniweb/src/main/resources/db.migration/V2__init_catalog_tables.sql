CREATE TABLE categories (
                            id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                            name        VARCHAR(150) NOT NULL UNIQUE,
                            status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
                            created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at  DATETIME NULL,
                            CONSTRAINT chk_categories_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
                          id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                          sku          VARCHAR(100) NOT NULL UNIQUE,
                          name         VARCHAR(200) NOT NULL,
                          category_id  BIGINT NOT NULL,
                          price        DECIMAL(18,2) NOT NULL DEFAULT 0,
                          stock        INT NOT NULL DEFAULT 0,
                          status       ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
                          created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at   DATETIME NULL,
                          CONSTRAINT fk_products_category FOREIGN KEY (category_id)
                              REFERENCES categories(id)
                              ON UPDATE RESTRICT ON DELETE RESTRICT,
                          CONSTRAINT chk_products_price CHECK (price >= 0),
                          CONSTRAINT chk_products_stock CHECK (stock >= 0),
                          CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_status      ON products(status);
CREATE INDEX idx_products_name_like   ON products(name);
