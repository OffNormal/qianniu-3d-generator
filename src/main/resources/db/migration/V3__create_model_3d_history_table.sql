-- 创建3D模型历史记录表
-- 用于存储用户生成的3D模型历史记录，支持文件和预览图的存储

CREATE TABLE model_3d_history (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    model_name VARCHAR(200) NOT NULL,
    model_file_path VARCHAR(500),
    model_file_data BLOB,
    preview_image_path VARCHAR(500),
    preview_image_data BLOB,
    file_size BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description CLOB,
    input_text CLOB,
    input_image_path VARCHAR(500),
    model_format VARCHAR(10),
    vertices_count INT,
    faces_count INT,
    download_count INT DEFAULT 0,
    client_ip VARCHAR(45),
    generation_time_seconds INT,
    complexity VARCHAR(20),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    original_task_id VARCHAR(50)
);

-- 创建索引
CREATE INDEX idx_model_name ON model_3d_history(model_name);
CREATE INDEX idx_create_time ON model_3d_history(create_time);
CREATE INDEX idx_status ON model_3d_history(status);
CREATE INDEX idx_client_ip ON model_3d_history(client_ip);
CREATE INDEX idx_original_task_id ON model_3d_history(original_task_id);
CREATE INDEX idx_download_count ON model_3d_history(download_count);
CREATE INDEX idx_model_format ON model_3d_history(model_format);
CREATE INDEX idx_file_size ON model_3d_history(file_size);

-- 插入示例数据（可选）
INSERT INTO model_3d_history (
    model_name, 
    description, 
    input_text, 
    model_format, 
    file_size, 
    vertices_count, 
    faces_count, 
    complexity, 
    client_ip,
    generation_time_seconds
) VALUES 
(
    '示例3D房屋模型', 
    '基于文本生成的简单房屋3D模型', 
    '一个简单的房屋，有门和窗户', 
    'GLB', 
    1024000, 
    2500, 
    1200, 
    'MEDIUM', 
    '127.0.0.1',
    45
),
(
    '示例汽车模型', 
    '现代风格的汽车3D模型', 
    '一辆现代风格的红色汽车', 
    'GLB', 
    2048000, 
    5000, 
    2800, 
    'HIGH', 
    '127.0.0.1',
    78
),
(
    '示例家具模型', 
    '简约风格的椅子模型', 
    '一把简约风格的木质椅子', 
    'GLB', 
    512000, 
    1200, 
    600, 
    'LOW', 
    '127.0.0.1',
    25
);