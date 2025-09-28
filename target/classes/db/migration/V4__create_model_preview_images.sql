-- 创建模型预览图片表，支持一个模型对应多张预览图片
CREATE TABLE model_preview_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_id NVARCHAR(50) NOT NULL,
    image_path NVARCHAR(500) NOT NULL,
    image_type NVARCHAR(20) DEFAULT 'PREVIEW',
    image_order INT DEFAULT 0,
    image_size BIGINT NULL,
    image_width INT NULL,
    image_height INT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NULL,
    
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

-- 添加注释
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'模型预览图片表，支持一个模型对应多张预览图片', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'model_preview_images';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'任务ID，关联model_tasks表', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'model_preview_images',
    @level2type = N'COLUMN', @level2name = N'task_id';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'图片文件路径', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'model_preview_images',
    @level2type = N'COLUMN', @level2name = N'image_path';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'图片类型：PREVIEW(预览图), TEXTURE(纹理图), NORMAL(法线图)', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'model_preview_images',
    @level2type = N'COLUMN', @level2name = N'image_type';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'图片显示顺序，从0开始', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'model_preview_images',
    @level2type = N'COLUMN', @level2name = N'image_order';