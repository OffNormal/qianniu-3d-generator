-- 创建模型预览图片表，支持一个模型对应多张预览图片
CREATE TABLE model_preview_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL,
    image_path VARCHAR(500) NOT NULL,
    image_type VARCHAR(20) DEFAULT 'PREVIEW',
    image_order INT DEFAULT 0,
    image_size BIGINT NULL,
    image_width INT NULL,
    image_height INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    
    -- 外键约束
    CONSTRAINT FK_model_preview_images_task_id 
        FOREIGN KEY (task_id) REFERENCES model_tasks(task_id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_model_preview_images_task_id ON model_preview_images(task_id);
CREATE INDEX idx_model_preview_images_order ON model_preview_images(task_id, image_order);

-- 为现有的模型任务迁移预览图片数据
INSERT INTO model_preview_images (task_id, image_path, image_type, image_order)
SELECT 
    task_id, 
    preview_image_path, 
    'PREVIEW', 
    0
FROM model_tasks 
WHERE preview_image_path IS NOT NULL AND preview_image_path != '';
