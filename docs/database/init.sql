-- 3D模型生成应用数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS qiniu_3d_generator 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE qiniu_3d_generator;

-- 生成任务表
CREATE TABLE generation_task (
    task_id VARCHAR(64) PRIMARY KEY COMMENT '任务ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    generation_type VARCHAR(20) NOT NULL COMMENT '生成类型：TEXT/IMAGE',
    input_content TEXT NOT NULL COMMENT '输入内容',
    input_hash VARCHAR(64) NOT NULL COMMENT '输入内容哈希',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/PROCESSING/COMPLETED/FAILED/CACHED',
    result_url VARCHAR(500) COMMENT '生成结果URL',
    model_file_path VARCHAR(500) COMMENT '模型文件路径',
    preview_image_url VARCHAR(500) COMMENT '预览图URL',
    external_task_id VARCHAR(100) COMMENT '第三方API任务ID',
    generation_params TEXT COMMENT '生成参数JSON',
    quality_score DECIMAL(3,2) COMMENT '质量评分(0-10)',
    user_rating INT COMMENT '用户评分(1-10)',
    processing_time BIGINT COMMENT '处理耗时(毫秒)',
    error_message TEXT COMMENT '错误信息',
    from_cache BOOLEAN DEFAULT FALSE COMMENT '是否来自缓存',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    complete_time DATETIME COMMENT '完成时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_input_hash (input_hash),
    INDEX idx_create_time (create_time),
    INDEX idx_quality_score (quality_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成任务表';

-- 质量评估表
CREATE TABLE quality_assessment (
    assessment_id VARCHAR(64) PRIMARY KEY COMMENT '评估ID',
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    model_url VARCHAR(500) NOT NULL COMMENT '模型URL',
    input_content TEXT NOT NULL COMMENT '输入内容',
    geometry_score DECIMAL(3,2) COMMENT '几何质量评分',
    texture_score DECIMAL(3,2) COMMENT '纹理质量评分',
    similarity_score DECIMAL(3,2) COMMENT '相似度评分',
    functionality_score DECIMAL(3,2) COMMENT '功能性评分',
    overall_score DECIMAL(3,2) COMMENT '综合评分',
    face_count INT COMMENT '面数',
    vertex_count INT COMMENT '顶点数',
    file_size BIGINT COMMENT '文件大小(字节)',
    quality_level VARCHAR(20) COMMENT '质量等级：HIGH/MEDIUM/LOW',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_overall_score (overall_score),
    INDEX idx_quality_level (quality_level),
    FOREIGN KEY (task_id) REFERENCES generation_task(task_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量评估表';

-- 缓存项表（用于持久化热门缓存）
CREATE TABLE cache_item (
    cache_key VARCHAR(128) PRIMARY KEY COMMENT '缓存键',
    input_content TEXT NOT NULL COMMENT '输入内容',
    generation_type VARCHAR(20) NOT NULL COMMENT '生成类型',
    result_url VARCHAR(500) NOT NULL COMMENT '结果URL',
    hit_count INT DEFAULT 0 COMMENT '命中次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_access_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    is_popular BOOLEAN DEFAULT FALSE COMMENT '是否热门',
    
    INDEX idx_generation_type (generation_type),
    INDEX idx_hit_count (hit_count),
    INDEX idx_expire_time (expire_time),
    INDEX idx_is_popular (is_popular)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缓存项表';

-- 用户统计表
CREATE TABLE user_statistics (
    user_id VARCHAR(64) PRIMARY KEY COMMENT '用户ID',
    total_tasks INT DEFAULT 0 COMMENT '总任务数',
    completed_tasks INT DEFAULT 0 COMMENT '完成任务数',
    failed_tasks INT DEFAULT 0 COMMENT '失败任务数',
    cache_hits INT DEFAULT 0 COMMENT '缓存命中数',
    avg_quality_score DECIMAL(3,2) COMMENT '平均质量评分',
    total_processing_time BIGINT DEFAULT 0 COMMENT '总处理时间',
    last_activity_time DATETIME COMMENT '最后活动时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_total_tasks (total_tasks),
    INDEX idx_avg_quality_score (avg_quality_score),
    INDEX idx_last_activity_time (last_activity_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计表';

-- 系统配置表
CREATE TABLE system_config (
    config_key VARCHAR(100) PRIMARY KEY COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_type VARCHAR(20) NOT NULL DEFAULT 'STRING' COMMENT '配置类型：STRING/INT/BOOLEAN/JSON',
    description VARCHAR(500) COMMENT '配置描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 插入默认配置
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('cache.default_expire_hours', '168', 'INT', '默认缓存过期时间(小时)'),
('cache.max_size', '10000', 'INT', '最大缓存数量'),
('evaluation.auto_evaluate', 'true', 'BOOLEAN', '是否自动评估'),
('evaluation.quality_threshold', '7.0', 'STRING', '质量阈值'),
('rate_limit.requests_per_minute', '60', 'INT', '每分钟请求限制'),
('rate_limit.requests_per_hour', '1000', 'INT', '每小时请求限制'),
('meshy.api.timeout', '60000', 'INT', 'API超时时间(毫秒)'),
('storage.max_file_size', '104857600', 'INT', '最大文件大小(字节)');

-- 创建视图：任务统计视图
CREATE VIEW v_task_statistics AS
SELECT 
    DATE(create_time) as task_date,
    generation_type,
    status,
    COUNT(*) as task_count,
    AVG(quality_score) as avg_quality_score,
    AVG(processing_time) as avg_processing_time,
    SUM(CASE WHEN from_cache = TRUE THEN 1 ELSE 0 END) as cache_hit_count
FROM generation_task 
GROUP BY DATE(create_time), generation_type, status;

-- 创建视图：用户活跃度视图
CREATE VIEW v_user_activity AS
SELECT 
    user_id,
    DATE(create_time) as activity_date,
    COUNT(*) as daily_tasks,
    AVG(quality_score) as daily_avg_quality,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
    SUM(CASE WHEN from_cache = TRUE THEN 1 ELSE 0 END) as cache_hits
FROM generation_task 
GROUP BY user_id, DATE(create_time);

-- 创建存储过程：更新用户统计
DELIMITER //
CREATE PROCEDURE UpdateUserStatistics(IN p_user_id VARCHAR(64))
BEGIN
    DECLARE v_total_tasks INT DEFAULT 0;
    DECLARE v_completed_tasks INT DEFAULT 0;
    DECLARE v_failed_tasks INT DEFAULT 0;
    DECLARE v_cache_hits INT DEFAULT 0;
    DECLARE v_avg_quality DECIMAL(3,2) DEFAULT 0;
    DECLARE v_total_time BIGINT DEFAULT 0;
    
    -- 计算统计数据
    SELECT 
        COUNT(*),
        SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN from_cache = TRUE THEN 1 ELSE 0 END),
        AVG(CASE WHEN quality_score IS NOT NULL THEN quality_score END),
        SUM(CASE WHEN processing_time IS NOT NULL THEN processing_time ELSE 0 END)
    INTO v_total_tasks, v_completed_tasks, v_failed_tasks, v_cache_hits, v_avg_quality, v_total_time
    FROM generation_task 
    WHERE user_id = p_user_id;
    
    -- 更新或插入用户统计
    INSERT INTO user_statistics (
        user_id, total_tasks, completed_tasks, failed_tasks, 
        cache_hits, avg_quality_score, total_processing_time, last_activity_time
    ) VALUES (
        p_user_id, v_total_tasks, v_completed_tasks, v_failed_tasks,
        v_cache_hits, v_avg_quality, v_total_time, NOW()
    ) ON DUPLICATE KEY UPDATE
        total_tasks = v_total_tasks,
        completed_tasks = v_completed_tasks,
        failed_tasks = v_failed_tasks,
        cache_hits = v_cache_hits,
        avg_quality_score = v_avg_quality,
        total_processing_time = v_total_time,
        last_activity_time = NOW();
END //
DELIMITER ;

-- 创建触发器：任务状态变更时更新用户统计
DELIMITER //
CREATE TRIGGER tr_task_status_update 
AFTER UPDATE ON generation_task
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        CALL UpdateUserStatistics(NEW.user_id);
    END IF;
END //
DELIMITER ;

-- 创建索引优化查询性能
CREATE INDEX idx_task_user_status ON generation_task(user_id, status);
CREATE INDEX idx_task_type_time ON generation_task(generation_type, create_time);
CREATE INDEX idx_assessment_score_time ON quality_assessment(overall_score, create_time);

COMMIT;