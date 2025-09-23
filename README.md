# 七牛云3D模型生成应用

基于DDD架构的智能3D模型生成平台，支持文本和图片生成3D模型，具备智能缓存、质量评估和API优化功能。

## 🚀 项目特性

- **多模态输入**：支持文本描述和图片上传生成3D模型
- **多API提供商**：支持混元3D、Meshy等多个API提供商，可自由选择或自动切换
- **智能缓存**：基于内容哈希的智能缓存机制，提升生成效率
- **质量评估**：自动化模型质量评估系统，多维度评分
- **API优化**：请求限流、超时控制、错误重试机制
- **容错机制**：支持API提供商故障自动切换，确保服务可用性
- **DDD架构**：领域驱动设计，清晰的分层架构
- **实时监控**：任务状态实时跟踪，用户统计分析

## 📁 项目结构

```
qiniu-3d-generator/
├── qiniu-3d-generator-app/          # 应用层 - 应用服务和配置
├── qiniu-3d-generator-domain/       # 领域层 - 核心业务逻辑
├── qiniu-3d-generator-infrastructure/ # 基础设施层 - 数据访问
├── qiniu-3d-generator-trigger/      # 触发器层 - 接口控制器
├── qiniu-3d-generator-types/        # 类型定义 - 通用类型
└── docs/                           # 文档目录
    ├── api/                        # API文档
    ├── database/                   # 数据库脚本
    └── design/                     # 设计文档
```

## 🛠️ 技术栈

- **框架**：Spring Boot 2.7+
- **数据库**：MySQL 8.0+
- **缓存**：Redis 6.0+
- **持久化**：MyBatis Plus
- **文档**：Swagger 3
- **构建工具**：Maven 3.6+
- **JDK版本**：Java 8+

## 🚀 快速开始

### 1. 环境准备

确保已安装以下软件：
- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 2. 数据库初始化

```bash
# 执行数据库初始化脚本
mysql -u root -p < docs/database/init.sql
```

### 3. 配置文件

修改 `qiniu-3d-generator-app/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qiniu_3d_generator
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password

# 第三方API配置
meshy:
  api:
    base-url: https://api.meshy.ai
    api-key: your_meshy_api_key
```

### 4. 启动应用

```bash
# 进入项目根目录
cd qiniu-3d-generator

# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run -pl qiniu-3d-generator-app
```

应用启动后访问：
- **应用地址**：http://localhost:8080
- **API文档**：http://localhost:8080/swagger-ui/index.html
- **健康检查**：http://localhost:8080/api/generation/health

## 📖 API文档

### 核心接口

#### 1. 文本生成3D模型
```http
POST /api/generation/text-to-3d
Content-Type: application/json

{
  "userId": "user123",
  "textPrompt": "一只可爱的小猫",
  "style": "cartoon",
  "quality": "high",
  "apiProvider": "hunyuan"  // 可选：指定API提供商 (hunyuan/meshy)
}
```

#### 2. 图片生成3D模型
```http
POST /api/generation/image-to-3d
Content-Type: multipart/form-data

userId: user123
imageFile: [图片文件]
style: realistic
quality: medium
apiProvider: hunyuan  // 可选：指定API提供商 (hunyuan/meshy)
```

#### 3. 获取可用API提供商
```http
GET /api/generation/providers
```

响应示例：
```json
{
  "availableProviders": ["hunyuan", "meshy"],
  "defaultProvider": "hunyuan",
  "fallbackProvider": "meshy",
  "fallbackEnabled": true
}

#### 4. 查询任务状态
```http
GET /api/generation/task/{taskId}/status
```

#### 5. 获取用户任务列表
```http
GET /api/generation/user/{userId}/tasks?page=0&size=10
```

### 质量评估接口

#### 评估3D模型质量
```http
POST /api/evaluation/evaluate
Content-Type: application/json

{
  "taskId": "task123",
  "modelUrl": "https://example.com/model.obj"
}
```

### 缓存管理接口

#### 智能缓存查找
```http
POST /api/cache/smart-find
Content-Type: application/json

{
  "inputContent": "一只可爱的小猫",
  "generationType": "TEXT"
}
```

## 🔌 API提供商配置

### 支持的API提供商

| 提供商 | 类型 | 支持功能 | 配置键 |
|--------|------|----------|--------|
| 混元3D | 腾讯云 | 文本转3D、图片转3D | `hunyuan` |
| Meshy | 第三方 | 文本转3D、图片转3D | `meshy` |

### 配置方式

在 `application.yml` 中配置API提供商：

```yaml
# 混元3D配置
hunyuan:
  api:
    base-url: https://hunyuan.tencentcloudapi.com
    secret-id: your_secret_id
    secret-key: your_secret_key
    region: ap-beijing

# Meshy配置  
meshy:
  api:
    base-url: https://api.meshy.ai
    api-key: your_meshy_api_key

# API提供商策略配置
api:
  provider:
    default: hunyuan              # 默认提供商
    fallback: meshy              # 备用提供商
    fallback-enabled: true       # 启用故障切换
    timeout-seconds: 30          # 请求超时时间
```

### 使用方式

#### 1. 使用默认提供商
不指定 `apiProvider` 参数，系统将使用配置的默认提供商：

```json
{
  "userId": "user123",
  "textPrompt": "一只可爱的小猫"
}
```

#### 2. 指定特定提供商
通过 `apiProvider` 参数指定使用的提供商：

```json
{
  "userId": "user123", 
  "textPrompt": "一只可爱的小猫",
  "apiProvider": "meshy"
}
```

#### 3. 故障自动切换
当指定的API提供商不可用时，系统会自动切换到备用提供商（如果启用了fallback）。

## 🔧 配置说明

### 应用配置

```yaml
app:
  cache:
    default-expire-hours: 168    # 默认缓存7天
    max-size: 10000             # 最大缓存数量
    similarity-threshold: 0.8    # 相似度阈值
  
  evaluation:
    auto-evaluate: true         # 自动评估
    quality-threshold: 7.0      # 质量阈值
  
  rate-limit:
    requests-per-minute: 60     # 每分钟请求限制
    requests-per-hour: 1000     # 每小时请求限制
```

### 第三方API配置

```yaml
meshy:
  api:
    base-url: https://api.meshy.ai
    api-key: ${MESHY_API_KEY}
    timeout: 60000              # 超时时间(毫秒)
    max-retries: 3              # 最大重试次数
```

## 📊 监控指标

应用提供以下监控指标：

- **生成成功率**：当前约40%（持续优化中）
- **平均响应时间**：文本生成 < 30s，图片生成 < 45s
- **缓存命中率**：目标 > 60%
- **质量评分**：平均分 > 7.0

## 🔍 故障排查

### 常见问题

1. **数据库连接失败**
   - 检查MySQL服务是否启动
   - 验证数据库连接配置
   - 确认数据库用户权限

2. **Redis连接失败**
   - 检查Redis服务状态
   - 验证Redis连接配置
   - 检查防火墙设置

3. **第三方API调用失败**
   - 验证API密钥配置
   - 检查网络连接
   - 查看API调用日志

### 日志查看

```bash
# 查看应用日志
tail -f logs/qiniu-3d-generator.log

# 查看错误日志
tail -f logs/error.log
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 联系方式

- **项目维护者**：开发团队
- **邮箱**：dev@example.com
- **问题反馈**：[GitHub Issues](https://github.com/your-org/qiniu-3d-generator/issues)

## 🔄 版本历史

- **v1.0.0** - 初始版本
  - 基础3D模型生成功能
  - 智能缓存系统
  - 质量评估模块
  - API优化功能

---

**注意**：本项目仍在持续开发中，部分功能可能需要进一步优化和完善。