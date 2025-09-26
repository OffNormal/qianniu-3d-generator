# 3D模型生成器 - API接口文档

## 1. 接口概述

### 1.1 基础信息
- **基础URL：** `http://localhost:8080/api/v1`
- **协议：** HTTP/HTTPS
- **数据格式：** JSON
- **字符编码：** UTF-8

### 1.2 通用响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2024-01-20T10:30:00Z"
}
```

### 1.3 状态码说明
| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | 服务暂时不可用 |

## 2. 核心接口

### 2.1 文本生成3D模型

#### 接口信息
- **URL：** `/models/generate/text`
- **方法：** POST
- **描述：** 根据文本描述生成3D模型

#### 请求参数
```json
{
  "text": "一只可爱的小猫",
  "complexity": "simple",
  "format": "obj",
  "options": {
    "color": "#FF6B6B",
    "size": "medium",
    "style": "cartoon"
  }
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| text | string | 是 | 文本描述，10-500字符 |
| complexity | string | 否 | 复杂度：simple/medium/complex，默认simple |
| format | string | 否 | 输出格式：obj/stl/ply，默认obj |
| options | object | 否 | 可选参数 |
| options.color | string | 否 | 颜色代码，默认#CCCCCC |
| options.size | string | 否 | 尺寸：small/medium/large，默认medium |
| options.style | string | 否 | 风格：realistic/cartoon/abstract，默认realistic |

#### 响应示例
```json
{
  "code": 200,
  "message": "模型生成成功",
  "data": {
    "taskId": "task_123456789",
    "status": "processing",
    "estimatedTime": 30
  },
  "timestamp": "2024-01-20T10:30:00Z"
}
```

### 2.2 图片生成3D模型

#### 接口信息
- **URL：** `/models/generate/image`
- **方法：** POST
- **描述：** 根据上传图片生成3D模型
- **Content-Type：** multipart/form-data

#### 请求参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| image | file | 是 | 图片文件，支持jpg/png/bmp，最大10MB |
| complexity | string | 否 | 复杂度：simple/medium/complex |
| format | string | 否 | 输出格式：obj/stl/ply |
| description | string | 否 | 辅助描述文本 |

#### 响应示例
```json
{
  "code": 200,
  "message": "图片上传成功，开始生成模型",
  "data": {
    "taskId": "task_987654321",
    "status": "processing",
    "estimatedTime": 60,
    "imageInfo": {
      "filename": "cat.jpg",
      "size": "1024x768",
      "fileSize": "2.3MB"
    }
  },
  "timestamp": "2024-01-20T10:30:00Z"
}
```

### 2.3 查询生成状态

#### 接口信息
- **URL：** `/models/status/{taskId}`
- **方法：** GET
- **描述：** 查询模型生成状态

#### 路径参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | string | 是 | 任务ID |

#### 响应示例
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "task_123456789",
    "status": "completed",
    "progress": 100,
    "result": {
      "modelId": "model_abc123",
      "downloadUrl": "/api/v1/models/download/model_abc123",
      "previewUrl": "/api/v1/models/preview/model_abc123",
      "modelInfo": {
        "vertices": 1234,
        "faces": 2468,
        "fileSize": "2.3MB",
        "format": "obj"
      }
    },
    "createdAt": "2024-01-20T10:30:00Z",
    "completedAt": "2024-01-20T10:30:45Z"
  },
  "timestamp": "2024-01-20T10:31:00Z"
}
```

#### 状态说明
| 状态 | 说明 |
|------|------|
| pending | 等待处理 |
| processing | 正在生成 |
| completed | 生成完成 |
| failed | 生成失败 |
| expired | 任务过期 |

### 2.4 下载模型文件

#### 接口信息
- **URL：** `/models/download/{modelId}`
- **方法：** GET
- **描述：** 下载生成的3D模型文件

#### 路径参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| modelId | string | 是 | 模型ID |

#### 查询参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| format | string | 否 | 下载格式，如果与生成时不同会进行转换 |

#### 响应
- **Content-Type：** application/octet-stream
- **Content-Disposition：** attachment; filename="model.obj"

### 2.5 获取模型预览

#### 接口信息
- **URL：** `/models/preview/{modelId}`
- **方法：** GET
- **描述：** 获取3D模型预览图

#### 路径参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| modelId | string | 是 | 模型ID |

#### 查询参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| angle | string | 否 | 预览角度：front/back/left/right/top/bottom |
| size | string | 否 | 图片尺寸：small/medium/large |

#### 响应
- **Content-Type：** image/png

## 3. 管理接口

### 3.1 获取生成历史

#### 接口信息
- **URL：** `/models/history`
- **方法：** GET
- **描述：** 获取用户的模型生成历史

#### 查询参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认10，最大50 |
| status | string | 否 | 状态过滤 |

#### 响应示例
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 25,
    "page": 1,
    "size": 10,
    "items": [
      {
        "taskId": "task_123456789",
        "modelId": "model_abc123",
        "type": "text",
        "input": "一只可爱的小猫",
        "status": "completed",
        "createdAt": "2024-01-20T10:30:00Z",
        "completedAt": "2024-01-20T10:30:45Z"
      }
    ]
  },
  "timestamp": "2024-01-20T10:31:00Z"
}
```

### 3.2 删除模型

#### 接口信息
- **URL：** `/models/{modelId}`
- **方法：** DELETE
- **描述：** 删除指定的3D模型

#### 路径参数
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| modelId | string | 是 | 模型ID |

#### 响应示例
```json
{
  "code": 200,
  "message": "模型删除成功",
  "data": null,
  "timestamp": "2024-01-20T10:31:00Z"
}
```

## 4. 系统接口

### 4.1 健康检查

#### 接口信息
- **URL：** `/health`
- **方法：** GET
- **描述：** 系统健康状态检查

#### 响应示例
```json
{
  "code": 200,
  "message": "系统运行正常",
  "data": {
    "status": "healthy",
    "version": "1.0.0",
    "uptime": "2d 5h 30m",
    "services": {
      "database": "healthy",
      "ai_service": "healthy",
      "file_storage": "healthy"
    }
  },
  "timestamp": "2024-01-20T10:31:00Z"
}
```

### 4.2 获取系统配置

#### 接口信息
- **URL：** `/config`
- **方法：** GET
- **描述：** 获取系统配置信息

#### 响应示例
```json
{
  "code": 200,
  "message": "获取配置成功",
  "data": {
    "maxFileSize": "10MB",
    "supportedFormats": ["obj", "stl", "ply"],
    "supportedImageTypes": ["jpg", "png", "bmp"],
    "maxTextLength": 500,
    "dailyLimit": 20,
    "estimatedTime": {
      "text_simple": 30,
      "text_medium": 60,
      "text_complex": 120,
      "image_simple": 60,
      "image_medium": 120,
      "image_complex": 300
    }
  },
  "timestamp": "2024-01-20T10:31:00Z"
}
```

## 5. 错误处理

### 5.1 错误响应格式
```json
{
  "code": 400,
  "message": "请求参数错误",
  "error": {
    "type": "VALIDATION_ERROR",
    "details": [
      {
        "field": "text",
        "message": "文本长度不能超过500字符"
      }
    ]
  },
  "timestamp": "2024-01-20T10:31:00Z"
}
```

### 5.2 常见错误类型
| 错误类型 | 说明 |
|----------|------|
| VALIDATION_ERROR | 参数验证错误 |
| FILE_TOO_LARGE | 文件过大 |
| UNSUPPORTED_FORMAT | 不支持的格式 |
| GENERATION_FAILED | 模型生成失败 |
| RATE_LIMIT_EXCEEDED | 超出频率限制 |
| TASK_NOT_FOUND | 任务不存在 |
| MODEL_NOT_FOUND | 模型不存在 |

## 6. 接口限制

### 6.1 频率限制
- 每个IP每分钟最多10次请求
- 每个用户每天最多生成20个模型
- 文件上传大小限制：10MB

### 6.2 超时设置
- 连接超时：30秒
- 读取超时：300秒（5分钟）
- 模型生成超时：600秒（10分钟）

## 7. 安全说明

### 7.1 输入验证
- 所有用户输入都会进行严格验证
- 文件类型和内容检查
- SQL注入和XSS防护

### 7.2 数据保护
- 用户上传的文件会在7天后自动删除
- 不会存储用户的个人敏感信息
- 所有API调用都会记录日志用于监控