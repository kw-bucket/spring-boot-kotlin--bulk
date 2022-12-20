CREATE TABLE IF NOT EXISTS jobs
(
	id                  INT NOT NULL AUTO_INCREMENT,
    status              VARCHAR(32) DEFAULT 'PENDING',
    as_of_date          TIMESTAMP,
    scheme              VARCHAR(32) NOT NULL,
    product_code        VARCHAR(32) NOT NULL,
    retry_count         SMALLINT UNSIGNED DEFAULT 0,
    process_x_required       BOOLEAN DEFAULT false,
    process_x_completed      BOOLEAN DEFAULT false,
    last_started_at     TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fetch_account_task
(
	id                  INT NOT NULL AUTO_INCREMENT,
	job_id              INT NOT NULL,
    offset              SMALLINT UNSIGNED,
    size                SMALLINT UNSIGNED,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	PRIMARY KEY (id),
    FOREIGN KEY (job_id) REFERENCES jobs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product_bucket_calculation_task
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    job_id              INT NOT NULL,
    client_key          VARCHAR(32) NOT NULL,
    product_code        VARCHAR(256) NOT NULL,
    completed           BOOLEAN DEFAULT false,
    result_code         VARCHAR(32),
    result_description  VARCHAR(256),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY (job_id) REFERENCES jobs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS interest_application_task
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    job_id              INT NOT NULL,
    account_key         VARCHAR(32) NOT NULL,
    completed           BOOLEAN DEFAULT false,
    result_code         VARCHAR(32),
    result_description  VARCHAR(256),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY (job_id) REFERENCES jobs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
