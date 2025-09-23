-- H2数据库初始化脚本（测试环境）

-- 生成任务表
CREATE TABLE IF NOT EXISTS generation_task (
    task_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    generation_type VARCHAR(20) NOT NULL,
    input_content TEXT NOT NULL,
    input_hash VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    result_url VARCHAR(500),
    model_file_path VARCHAR(500),
    preview_image_url VARCHAR(500),
    external_task_id VARCHAR(100),
    generation_params TEXT,
    quality_score DECIMAL(3,2),
    user_rating TINYINT,
    processing_time BIGINT,
    error_message TEXT,
    from_cache BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_user_id ON generation_task(user_id);
CREATE INDEX IF NOT EXISTS idx_status ON generation_task(status);
CREATE INDEX IF NOT EXISTS idx_input_hash ON generation_task(input_hash);
CREATE INDEX IF NOT EXISTS idx_create_time ON generation_task(create_time);

-- 缓存表
CREATE TABLE IF NOT EXISTS cache_item (
    cache_key VARCHAR(128) PRIMARY KEY,
    input_content TEXT NOT NULL,
    generation_type VARCHAR(20) NOT NULL,
    result_url VARCHAR(500) NOT NULL,
    preview_image_url VARCHAR(500),
    quality_score DECIMAL(3,2),
    hit_count INT DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_hit_time TIMESTAMP,
    expire_time TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_generation_type ON cache_item(generation_type);
CREATE INDEX IF NOT EXISTS idx_create_time_cache ON cache_item(create_time);
CREATE INDEX IF NOT EXISTS idx_expire_time ON cache_item(expire_time);