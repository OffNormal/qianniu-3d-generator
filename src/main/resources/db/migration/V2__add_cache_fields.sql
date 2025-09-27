-- 添加缓存相关字段到 model_tasks 表
ALTER TABLE model_tasks ADD reference_count INT DEFAULT 1;
ALTER TABLE model_tasks ADD last_accessed DATETIME2 NULL;
ALTER TABLE model_tasks ADD access_count INT DEFAULT 1;
ALTER TABLE model_tasks ADD file_signature NVARCHAR(64) NULL;
ALTER TABLE model_tasks ADD input_hash NVARCHAR(64) NULL;

-- 为现有记录设置默认值
UPDATE model_tasks 
SET reference_count = 1, 
    access_count = 1, 
    last_accessed = COALESCE(completed_at, updated_at, created_at)
WHERE reference_count IS NULL;

-- 添加优化索引
CREATE INDEX idx_similarity_lookup ON model_tasks(type, status, complexity, output_format);
CREATE INDEX idx_last_accessed ON model_tasks(last_accessed);
CREATE INDEX idx_access_count ON model_tasks(access_count);
CREATE INDEX idx_file_signature ON model_tasks(file_signature);
CREATE INDEX idx_input_hash ON model_tasks(input_hash);
CREATE INDEX idx_reference_count ON model_tasks(reference_count);

-- 创建缓存专用表
CREATE TABLE model_cache_metadata (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_id NVARCHAR(50) NOT NULL,
    input_hash NVARCHAR(64) NOT NULL,
    file_signature NVARCHAR(64) NULL,
    reference_count INT DEFAULT 1,
    last_accessed DATETIME2 NULL,
    access_count INT DEFAULT 1,
    created_at DATETIME2 NULL,
    updated_at DATETIME2 NULL
);

-- 为 model_cache_metadata 表添加索引
CREATE UNIQUE INDEX uk_task_id ON model_cache_metadata(task_id);
CREATE INDEX idx_input_hash_cache ON model_cache_metadata(input_hash);
CREATE INDEX idx_last_accessed_cache ON model_cache_metadata(last_accessed);
CREATE INDEX idx_access_count_cache ON model_cache_metadata(access_count);
CREATE INDEX idx_file_signature_cache ON model_cache_metadata(file_signature);

-- 添加默认值约束
ALTER TABLE model_cache_metadata ADD CONSTRAINT DF_model_cache_metadata_created_at DEFAULT GETDATE() FOR created_at;
ALTER TABLE model_cache_metadata ADD CONSTRAINT DF_model_cache_metadata_updated_at DEFAULT GETDATE() FOR updated_at;
ALTER TABLE model_cache_metadata ADD CONSTRAINT DF_model_cache_metadata_last_accessed DEFAULT GETDATE() FOR last_accessed;