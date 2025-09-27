-- 创建 model_tasks 表
CREATE TABLE model_tasks (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_id NVARCHAR(50) NOT NULL UNIQUE,
    type NVARCHAR(20) NOT NULL,
    input_text NTEXT NULL,
    input_image_path NVARCHAR(500) NULL,
    complexity NVARCHAR(20) NULL,
    output_format NVARCHAR(10) NULL,
    status NVARCHAR(20) NOT NULL,
    progress INT DEFAULT 0,
    model_id NVARCHAR(50) NULL,
    model_file_path NVARCHAR(500) NULL,
    preview_image_path NVARCHAR(500) NULL,
    vertices_count INT NULL,
    faces_count INT NULL,
    file_size BIGINT NULL,
    error_message NTEXT NULL,
    created_at DATETIME2 DEFAULT GETDATE() NOT NULL,
    updated_at DATETIME2 DEFAULT GETDATE(),
    completed_at DATETIME2 NULL,
    client_ip NVARCHAR(45) NULL,
    cached BIT DEFAULT 0,
    cache_hit_count INT DEFAULT 0
);

-- 创建索引
CREATE INDEX idx_task_id ON model_tasks(task_id);
CREATE INDEX idx_status ON model_tasks(status);
CREATE INDEX idx_type ON model_tasks(type);
CREATE INDEX idx_created_at ON model_tasks(created_at);
CREATE INDEX idx_status_type ON model_tasks(status, type);

-- 添加更新时间触发器的替代方案（使用默认约束）
ALTER TABLE model_tasks ADD CONSTRAINT DF_model_tasks_updated_at DEFAULT GETDATE() FOR updated_at;