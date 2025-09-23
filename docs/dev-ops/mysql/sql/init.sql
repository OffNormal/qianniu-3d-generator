-- 创建数据库
CREATE DATABASE IF NOT EXISTS qiniu_3d_generator DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE qiniu_3d_generator;

-- 生成任务表
CREATE TABLE IF NOT EXISTS generation_task (
    task_id VARCHAR(64) PRIMARY KEY COMMENT '任务ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    generation_type VARCHAR(20) NOT NULL COMMENT '生成类型：TEXT/IMAGE',
    input_content TEXT NOT NULL COMMENT '输入内容（文本描述或图片URL）',
    input_hash VARCHAR(64) COMMENT '输入内容哈希（用于缓存匹配）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/PROCESSING/COMPLETED/FAILED/CACHED',
    result_url VARCHAR(500) COMMENT '生成结果URL',
    model_file_path VARCHAR(500) COMMENT '模型文件路径',
    preview_image_url VARCHAR(500) COMMENT '预览图URL',
    external_task_id VARCHAR(100) COMMENT '第三方API任务ID',
    generation_params TEXT COMMENT '生成参数（JSON格式）',
    quality_score DECIMAL(3,2) COMMENT '质量评分（0-1）',
    user_rating TINYINT COMMENT '用户评分（1-5）',
    processing_time BIGINT COMMENT '处理耗时（秒）',
    error_message TEXT COMMENT '错误信息',
    from_cache BOOLEAN DEFAULT FALSE COMMENT '是否来自缓存',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    complete_time DATETIME COMMENT '完成时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_input_hash (input_hash),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='3D模型生成任务表';

-- 缓存表
CREATE TABLE IF NOT EXISTS cache_item (
    cache_key VARCHAR(128) PRIMARY KEY COMMENT '缓存键',
    input_content TEXT NOT NULL COMMENT '输入内容',
    generation_type VARCHAR(20) NOT NULL COMMENT '生成类型',
    result_url VARCHAR(500) NOT NULL COMMENT '结果URL',
    preview_image_url VARCHAR(500) COMMENT '预览图URL',
    quality_score DECIMAL(3,2) COMMENT '质量评分',
    hit_count INT DEFAULT 0 COMMENT '命中次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_hit_time DATETIME COMMENT '最后命中时间',
    expire_time DATETIME COMMENT '过期时间',
    
    INDEX idx_generation_type (generation_type),
    INDEX idx_create_time (create_time),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缓存表';

-- 评估记录表
CREATE TABLE IF NOT EXISTS evaluation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    model_url VARCHAR(500) NOT NULL COMMENT '模型URL',
    quality_score DECIMAL(3,2) NOT NULL COMMENT '质量评分（0-1）',
    evaluation_details TEXT COMMENT '评估详情（JSON格式）',
    evaluation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评估时间',
    
    UNIQUE KEY uk_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_quality_score (quality_score),
    INDEX idx_evaluation_time (evaluation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型质量评估记录表';

-- 用户评分表
CREATE TABLE IF NOT EXISTS user_rating (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    rating TINYINT NOT NULL COMMENT '用户评分（1-5）',
    comment TEXT COMMENT '评价内容',
    rating_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
    
    UNIQUE KEY uk_task_user (task_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_rating (rating),
    INDEX idx_rating_time (rating_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户评分表';