# 缓存系统 API 文档

## 概述

本文档描述了3D模型生成系统的缓存管理和监控API接口。缓存系统提供了智能缓存、性能监控、健康检查和管理功能。

## 基础信息

- **Base URL**: `/api/cache`
- **Content-Type**: `application/json`
- **认证**: 需要有效的API Token

## API 接口列表

### 1. 健康检查接口

#### 1.1 快速健康检查
```http
GET /api/cache/health
```

**响应示例**:
```json
{
  "status": "HEALTHY",
  "timestamp": "2024-01-20T10:30:00Z",
  "message": "Cache system is operating normally"
}
```

#### 1.2 详细健康检查
```http
GET /api/cache/health/detailed
```

**响应示例**:
```json
{
  "overallStatus": "HEALTHY",
  "timestamp": "2024-01-20T10:30:00Z",
  "performanceCheck": {
    "status": "HEALTHY",
    "hitRate": 0.85,
    "averageResponseTime": 45.2,
    "details": "Performance within acceptable range"
  },
  "capacityCheck": {
    "status": "WARNING",
    "currentCapacity": 8500,
    "maxCapacity": 10000,
    "usagePercentage": 85.0,
    "details": "Capacity approaching limit"
  },
  "recommendations": [
    "Consider increasing cache capacity",
    "Monitor hit rate trends"
  ]
}
```

#### 1.3 完整健康检查
```http
GET /api/cache/health/full
```

### 2. 缓存统计接口

#### 2.1 实时指标
```http
GET /api/cache/stats/realtime
```

**响应示例**:
```json
{
  "totalHits": 15420,
  "totalMisses": 2830,
  "hitRate": 0.845,
  "totalOperations": 18250,
  "averageResponseTime": 42.5,
  "cacheSize": 8500,
  "timestamp": "2024-01-20T10:30:00Z"
}
```

#### 2.2 历史指标
```http
GET /api/cache/stats/historical?hours=24
```

**查询参数**:
- `hours` (可选): 查询时间范围，默认24小时

#### 2.3 性能报告
```http
GET /api/cache/stats/performance
```

**响应示例**:
```json
{
  "reportPeriod": "24h",
  "overallHitRate": 0.845,
  "peakHitRate": 0.92,
  "lowHitRate": 0.78,
  "averageResponseTime": 42.5,
  "peakResponseTime": 156.2,
  "totalRequests": 18250,
  "cacheEfficiency": "HIGH",
  "recommendations": [
    "Cache performance is excellent",
    "Consider expanding popular content caching"
  ],
  "timestamp": "2024-01-20T10:30:00Z"
}
```

#### 2.4 热点分析
```http
GET /api/cache/stats/hotspots?limit=10
```

**查询参数**:
- `limit` (可选): 返回热点数量，默认10

**响应示例**:
```json
[
  {
    "cacheKey": "text_generation_model_v2",
    "hitCount": 1250,
    "lastAccessed": "2024-01-20T10:25:00Z",
    "averageResponseTime": 35.2,
    "popularity": "HIGH"
  }
]
```

#### 2.5 趋势分析
```http
GET /api/cache/stats/trends?period=7d
```

**查询参数**:
- `period` (可选): 分析周期 (1d, 7d, 30d)，默认7d

### 3. 缓存管理接口

#### 3.1 强制清理
```http
POST /api/cache/management/cleanup/force
```

**响应示例**:
```json
{
  "cleanedCount": 150,
  "freedSpace": "2.5GB",
  "duration": "00:02:15",
  "timestamp": "2024-01-20T10:30:00Z"
}
```

#### 3.2 获取清理候选
```http
GET /api/cache/management/cleanup/candidates?limit=50
```

#### 3.3 强制淘汰任务
```http
DELETE /api/cache/management/evict/{taskId}
```

**路径参数**:
- `taskId`: 要淘汰的任务ID

#### 3.4 清理统计
```http
GET /api/cache/management/cleanup/stats
```

### 4. 缓存预热接口

#### 4.1 执行预热
```http
POST /api/cache/warmup/execute
```

**请求体**:
```json
{
  "strategy": "POPULAR_TASKS",
  "limit": 500
}
```

**策略选项**:
- `POPULAR_TASKS`: 热门任务预热
- `TIME_PATTERN`: 时间模式预热
- `USER_BEHAVIOR`: 用户行为预热
- `SIMILAR_TASKS`: 相似任务预热

#### 4.2 预热统计
```http
GET /api/cache/warmup/stats
```

**响应示例**:
```json
{
  "lastWarmupTime": "2024-01-20T08:00:00Z",
  "totalWarmedTasks": 450,
  "warmupDuration": "00:05:30",
  "warmupEfficiency": 0.89,
  "nextScheduledWarmup": "2024-01-20T10:00:00Z",
  "warmupStrategy": "POPULAR_TASKS"
}
```

#### 4.3 获取预热候选
```http
GET /api/cache/warmup/candidates?strategy=POPULAR_TASKS&limit=100
```

### 5. 系统管理接口

#### 5.1 重置指标
```http
POST /api/cache/system/metrics/reset
```

#### 5.2 获取缓存状态
```http
GET /api/cache/system/status
```

**响应示例**:
```json
{
  "systemStatus": "RUNNING",
  "cacheEnabled": true,
  "currentCapacity": 8500,
  "maxCapacity": 10000,
  "usagePercentage": 85.0,
  "activeConnections": 25,
  "uptime": "15d 8h 30m",
  "version": "1.0.0"
}
```

#### 5.3 获取缓存配置
```http
GET /api/cache/system/config
```

## 错误响应

所有API在出错时返回统一的错误格式：

```json
{
  "error": {
    "code": "CACHE_ERROR_001",
    "message": "Cache operation failed",
    "details": "Detailed error description",
    "timestamp": "2024-01-20T10:30:00Z"
  }
}
```

### 常见错误码

| 错误码 | 描述 | HTTP状态码 |
|--------|------|------------|
| CACHE_ERROR_001 | 缓存操作失败 | 500 |
| CACHE_ERROR_002 | 缓存不可用 | 503 |
| CACHE_ERROR_003 | 参数无效 | 400 |
| CACHE_ERROR_004 | 资源未找到 | 404 |
| CACHE_ERROR_005 | 权限不足 | 403 |

## 使用示例

### 监控缓存健康状态

```bash
# 检查缓存健康状态
curl -X GET "http://localhost:8080/api/cache/health" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 获取详细健康报告
curl -X GET "http://localhost:8080/api/cache/health/detailed" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 查看缓存性能

```bash
# 获取实时统计
curl -X GET "http://localhost:8080/api/cache/stats/realtime" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 获取性能报告
curl -X GET "http://localhost:8080/api/cache/stats/performance" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 执行缓存管理

```bash
# 强制清理缓存
curl -X POST "http://localhost:8080/api/cache/management/cleanup/force" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 执行缓存预热
curl -X POST "http://localhost:8080/api/cache/warmup/execute" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"strategy": "POPULAR_TASKS", "limit": 500}'
```

## 监控建议

### 关键指标监控

1. **命中率**: 应保持在70%以上
2. **响应时间**: 平均响应时间应低于100ms
3. **容量使用率**: 应保持在85%以下
4. **错误率**: 应保持在5%以下

### 告警设置

建议为以下情况设置告警：

- 命中率低于60%
- 平均响应时间超过200ms
- 容量使用率超过85%
- 错误率超过5%
- 缓存服务不可用

### 性能优化建议

1. **定期监控热点**: 使用热点分析API识别高频访问内容
2. **优化预热策略**: 根据用户行为调整预热参数
3. **容量规划**: 根据趋势分析进行容量规划
4. **清理策略调优**: 根据业务需求调整清理策略

## 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.0.0 | 2024-01-20 | 初始版本，包含基础缓存管理功能 |

## 联系方式

如有问题或建议，请联系：
- 邮箱: dev@qiniu.com
- 文档: https://docs.qiniu.com/cache-api