ALTER TABLE job ADD is_ax_enabled BOOLEAN DEFAULT false;

ALTER TABLE a_task RENAME COLUMN product_bucket_completed TO a1_completed;
ALTER TABLE a_task RENAME COLUMN product_bucket_result_code TO a1_result_code;
ALTER TABLE a_task RENAME COLUMN product_bucket_result_description TO a1_result_description;

ALTER TABLE a_task RENAME COLUMN collection_completed TO a2_completed;
ALTER TABLE a_task RENAME COLUMN collection_result_code TO a2_result_code;
ALTER TABLE a_task RENAME COLUMN collection_result_description TO a2_result_description;

ALTER TABLE a_task ADD x_process_required BOOLEAN DEFAULT false;

CREATE TABLE IF NOT EXISTS x_task
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    job_id              INT NOT NULL,
    a_task_id  BIGINT NOT NULL,
    account_id  VARCHAR(64) NOT NULL,
    branch_key          VARCHAR(32) NOT NULL,
    branch_name         VARCHAR(32) NOT NULL,
    completed           BOOLEAN DEFAULT false,
    result_code         VARCHAR(32),
    result_description  TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY (job_id) REFERENCES job(id),
    FOREIGN KEY (a_task_id) REFERENCES a_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
