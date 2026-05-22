CREATE DATABASE IF NOT EXISTS mlops_platform
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mlops_platform;

CREATE TABLE IF NOT EXISTS model_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_name VARCHAR(128) NOT NULL,
    script_path VARCHAR(512) NOT NULL,
    description VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

INSERT INTO model_asset (asset_name, script_path, description)
SELECT 'Vision Remote Sensing Mock Evaluator',
       '../scripts/mock_process.py',
       'Python mock evaluator with fixed 8-day window, final-row averages, scientific slope notation, and masked anomaly map.'
WHERE NOT EXISTS (
    SELECT 1 FROM model_asset WHERE asset_name = 'Vision Remote Sensing Mock Evaluator'
);

