CREATE DATABASE IF NOT EXISTS mlops_platform
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mlops_platform;

CREATE TABLE IF NOT EXISTS model_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_name VARCHAR(128) NOT NULL,
    script_path VARCHAR(512),
    original_filename VARCHAR(255),
    content_type VARCHAR(128),
    file_size BIGINT,
    file_data LONGBLOB,
    description VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME
);

SET @schema_name = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'model_asset' AND COLUMN_NAME = 'script_path') = 1,
    'ALTER TABLE model_asset MODIFY COLUMN script_path VARCHAR(512) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'model_asset' AND COLUMN_NAME = 'original_filename') = 0,
    'ALTER TABLE model_asset ADD COLUMN original_filename VARCHAR(255)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'model_asset' AND COLUMN_NAME = 'content_type') = 0,
    'ALTER TABLE model_asset ADD COLUMN content_type VARCHAR(128)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'model_asset' AND COLUMN_NAME = 'file_size') = 0,
    'ALTER TABLE model_asset ADD COLUMN file_size BIGINT',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'model_asset' AND COLUMN_NAME = 'file_data') = 0,
    'ALTER TABLE model_asset ADD COLUMN file_data LONGBLOB',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'model_asset' AND COLUMN_NAME = 'updated_at') = 0,
    'ALTER TABLE model_asset ADD COLUMN updated_at DATETIME',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS evaluation_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(128) NOT NULL,
    target_date DATE NOT NULL,
    mask_min DOUBLE NOT NULL,
    mask_max DOUBLE NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(1024),
    execution_log TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    finished_at DATETIME
);

CREATE TABLE IF NOT EXISTS task_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL UNIQUE,
    csv_path VARCHAR(512) NOT NULL,
    trend_chart_path VARCHAR(512) NOT NULL,
    anomaly_map_path VARCHAR(512) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_result_task
        FOREIGN KEY (task_id)
        REFERENCES evaluation_task (id)
        ON DELETE CASCADE
);
