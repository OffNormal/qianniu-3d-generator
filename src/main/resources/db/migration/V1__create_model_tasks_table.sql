-- 创建 model_tasks 表
CREATE TABLE model_tasks (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    input_text CLOB NULL,
    input_image_path VARCHAR(500) NULL,
    complexity VARCHAR(20) NULL,
    output_format VARCHAR(10) NULL,
    status VARCHAR(20) NOT NULL,
    progress INT DEFAULT 0,
    model_id VARCHAR(50) NULL,
    model_file_path VARCHAR(500) NULL,
    preview_image_path VARCHAR(500) NULL,
    vertices_count INT NULL,
    faces_count INT NULL,
    file_size BIGINT NULL,
    error_message CLOB NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    client_ip VARCHAR(45) NULL,
    cached BOOLEAN DEFAULT FALSE,
    cache_hit_count INT DEFAULT 0
);

-- 创建索引
CREATE INDEX idx_task_id ON model_tasks(task_id);
CREATE INDEX idx_status ON model_tasks(status);
CREATE INDEX idx_type ON model_tasks(type);
CREATE INDEX idx_created_at ON model_tasks(created_at);
CREATE INDEX idx_status_type ON model_tasks(status, type);