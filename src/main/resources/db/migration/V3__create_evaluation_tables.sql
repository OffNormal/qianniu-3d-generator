-- 创建任务评估记录表
CREATE TABLE task_evaluation (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL,
    prompt CLOB,
    result_format VARCHAR(20),
    status VARCHAR(20),
    submit_time TIMESTAMP,
    complete_time TIMESTAMP,
    duration_seconds INT,
    file_size_kb INT,
    user_rating TINYINT,
    download_count INT DEFAULT 0,
    preview_count INT DEFAULT 0,
    client_ip VARCHAR(45),
    error_message CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建系统指标统计表
CREATE TABLE system_metrics (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    metric_date DATE NOT NULL,
    total_tasks INT DEFAULT 0,
    success_tasks INT DEFAULT 0,
    failed_tasks INT DEFAULT 0,
    avg_duration_seconds DECIMAL(10,2) DEFAULT 0,
    total_users INT DEFAULT 0,
    avg_rating DECIMAL(3,2) DEFAULT 0,
    total_downloads INT DEFAULT 0,
    total_previews INT DEFAULT 0,
    success_rate DECIMAL(5,4) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 为 task_evaluation 表创建索引
CREATE INDEX idx_task_evaluation_job_id ON task_evaluation(job_id);
CREATE INDEX idx_task_evaluation_status ON task_evaluation(status);
CREATE INDEX idx_task_evaluation_submit_time ON task_evaluation(submit_time);
CREATE INDEX idx_task_evaluation_rating ON task_evaluation(user_rating);
CREATE INDEX idx_task_evaluation_client_ip ON task_evaluation(client_ip);

-- 为 system_metrics 表创建索引
CREATE UNIQUE INDEX uk_system_metrics_date ON system_metrics(metric_date);
CREATE INDEX idx_system_metrics_created_at ON system_metrics(created_at);
