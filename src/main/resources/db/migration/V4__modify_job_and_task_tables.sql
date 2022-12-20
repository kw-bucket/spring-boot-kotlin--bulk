ALTER TABLE jobs RENAME COLUMN process_x_required TO process_a_required;
ALTER TABLE jobs DROP COLUMN process_x_completed;

ALTER TABLE product_bucket_calculation_task DROP INDEX product_bucket_tasks_client_key;
ALTER TABLE product_bucket_calculation_task DROP INDEX product_bucket_tasks_product_code;

DROP TABLE fetch_account_task;

RENAME TABLE jobs TO job;
RENAME TABLE product_bucket_calculation_task TO a_task;
RENAME TABLE interest_application_task TO b_task;

ALTER TABLE a_task RENAME COLUMN completed TO product_bucket_completed;
ALTER TABLE a_task RENAME COLUMN result_code TO product_bucket_result_code;
ALTER TABLE a_task RENAME COLUMN result_description TO product_bucket_result_description;

ALTER TABLE a_task ADD collection_completed BOOLEAN DEFAULT false;
ALTER TABLE a_task ADD collection_result_code VARCHAR(32);
ALTER TABLE a_task ADD collection_result_description TEXT;

CREATE INDEX Idx_Scheme_ProductCode ON job (scheme, product_code);
CREATE INDEX Idx_ProductCode_AsOfDate ON job (product_code, as_of_date);

CREATE INDEX Idx_ProductBucketCompleted_JobId ON a_task (product_bucket_completed, job_id);
CREATE INDEX Idx_CollectionFlagCompleted_JobId ON a_task (collection_completed, job_id);

CREATE INDEX Idx_Completed_JobId ON b_task (completed, job_id);

DROP TABLE IF EXISTS collection_flag_tasks;
