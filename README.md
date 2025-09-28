# 七牛3D模型生成器 🎨

[![Java](https://img.shields.io/badge/Java-11-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.15-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

一个基于腾讯混元AI的轻量级3D模型生成器，支持通过文本描述或图片上传生成高质量3D模型。

## 📋 目录

- [✨ 功能特性](#-功能特性)
- [🏗️ 技术栈](#️-技术栈)
- [🏛️ 系统架构设计](#️-系统架构设计)
- [🚀 快速开始](#-快速开始)
- [📖 API文档](#-api文档)
- [🎮 使用示例](#-使用示例)
- [📁 项目结构](#-项目结构)
- [🔧 配置说明](#-配置说明)
- [🧪 测试](#-测试)
- [📊 性能指标](#-性能指标)
- [👥 项目分工](#-项目分工)
- [🤝 贡献指南](#-贡献指南)
- [📝 更新日志](#-更新日志)
- [📄 许可证](#-许可证)
- [🙏 致谢](#-致谢)
- [📞 联系我们](#-联系我们)

## ✨ 功能特性

### 🎯 核心功能
- **文本生成3D模型** - 通过自然语言描述生成3D模型
- **图片生成3D模型** - 上传图片自动识别并生成对应3D模型
- **多格式支持** - 支持OBJ、STL、PLY等主流3D格式
- **实时预览** - 在线3D模型预览和交互

### 🛠️ 辅助功能
- **任务管理** - 异步任务处理，支持任务状态查询和历史记录
- **文件下载** - 支持生成模型的下载和预览图片
- **参数配置** - 可调节模型复杂度、格式、艺术风格等参数
- **缓存系统** - 智能缓存机制，提升相似请求的响应速度
- **管理面板** - 系统监控、性能分析和用户行为统计
- **定时清理** - 自动清理过期文件和任务记录
- **多环境支持** - 开发、测试、生产环境配置分离
- **API接口** - 完整的RESTful API支持

## 🏗️ 技术栈

### 后端技术
- **Java 11** - 核心开发语言
- **Spring Boot 2.6.15** - 应用框架
- **Spring Data JPA** - 数据持久化
- **MyBatis** - SQL映射框架
- **MySQL 8.0** - 主数据库
- **H2 Database** - 开发环境数据库

### AI服务
- **腾讯云混元AI** - 3D模型生成引擎
- **腾讯云AI艺术** - 图像处理服务

### 其他组件
- **Jackson** - JSON处理
- **Commons FileUpload** - 文件上传
- **SLF4J** - 日志框架
- **JUnit 5** - 单元测试

## 🏛️ 系统架构设计

### 整体架构
本项目采用经典的三层架构模式，结合Spring Boot框架构建，实现了高内聚、低耦合的系统设计。

```
┌─────────────────────────────────────────────────────────────┐
│                    前端展示层 (Presentation Layer)              │
├─────────────────────────────────────────────────────────────┤
│  Web界面 (HTML/CSS/JS)  │  RESTful API接口  │  管理面板        │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                    业务逻辑层 (Business Layer)                 │
├─────────────────────────────────────────────────────────────┤
│  控制器层 (Controller)  │  服务层 (Service)  │  任务调度        │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                    数据访问层 (Data Access Layer)             │
├─────────────────────────────────────────────────────────────┤
│  数据仓库 (Repository)  │  实体映射 (Entity)  │  缓存管理       │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                    外部服务层 (External Services)             │
├─────────────────────────────────────────────────────────────┤
│  腾讯云AI3D服务  │  文件存储系统  │  数据库 (MySQL/H2)        │
└─────────────────────────────────────────────────────────────┘
```

### 核心模块规格

#### 1. 控制器模块 (Controller Layer)
**功能职责**: 处理HTTP请求，参数验证，响应格式化
- **ModelGenerationController**: 3D模型生成接口控制器
  - 输入: 文本描述或图片URL
  - 输出: 任务ID和生成状态
  - 规格: 支持异步处理，最大并发50个请求
- **Model3DHistoryController**: 历史记录管理控制器
  - 功能: 查询、删除历史记录，分页展示
  - 规格: 支持按状态、时间范围筛选
- **AdminDashboardController**: 系统管理面板控制器
  - 功能: 系统监控、性能统计、缓存管理
  - 规格: 提供实时数据和图表展示
- **FileUploadController**: 文件上传控制器
  - 功能: 处理图片上传，格式验证
  - 规格: 支持最大10MB文件，JPG/PNG/GIF格式

#### 2. 服务模块 (Service Layer)
**功能职责**: 核心业务逻辑处理，外部服务集成
- **ModelGenerationService**: 模型生成核心服务
  - 功能: 协调AI服务调用，任务状态管理
  - 规格: 支持文本和图片两种输入模式，超时时间600秒
  - 性能: 文本生成<30秒，图片生成<60秒
- **TencentAi3dClient**: 腾讯云AI3D客户端
  - 功能: 封装腾讯云API调用，错误处理和重试机制
  - 规格: 支持3次重试，连接超时60秒
- **CacheService**: 缓存管理服务
  - 功能: 智能缓存相似请求，提升响应速度
  - 规格: 基于内存缓存，TTL为1小时，最大缓存1000个条目

#### 3. 数据访问模块 (Repository Layer)
**功能职责**: 数据持久化，数据库操作封装
- **Model3DHistoryRepository**: 历史记录数据访问
  - 功能: CRUD操作，复杂查询支持
  - 规格: 支持分页、排序、条件筛选
- **GenerationTaskRepository**: 任务数据访问
  - 功能: 任务状态跟踪，批量操作
  - 规格: 支持事务管理，数据一致性保证

#### 4. 实体模块 (Entity Layer)
**功能职责**: 数据模型定义，对象关系映射
- **Model3DHistory**: 历史记录实体
  - 字段: ID、用户输入、生成参数、文件路径、状态、时间戳
  - 约束: 非空验证，长度限制，状态枚举
- **GenerationTask**: 生成任务实体
  - 字段: 任务ID、类型、状态、进度、错误信息
  - 约束: 唯一性约束，状态流转控制

#### 5. 配置模块 (Configuration Layer)
**功能职责**: 系统配置管理，Bean定义和装配
- **TencentCloudConfig**: 腾讯云服务配置
  - 功能: API密钥管理，服务端点配置
  - 规格: 支持多环境配置，敏感信息加密
- **WebMvcConfig**: Web MVC配置
  - 功能: 跨域配置，拦截器设置，静态资源映射
  - 规格: 支持CORS，文件上传限制

#### 6. 调度模块 (Scheduler Layer)
**功能职责**: 定时任务管理，系统维护
- **TaskCleanupScheduler**: 任务清理调度器
  - 功能: 定期清理过期文件和任务记录
  - 规格: 每日凌晨2点执行，清理7天前的数据

### 数据流设计

#### 文本生成3D模型流程
```
用户输入文本 → 参数验证 → 创建任务记录 → 调用腾讯云API → 
轮询任务状态 → 下载模型文件 → 生成预览图 → 更新任务状态 → 返回结果
```

#### 图片生成3D模型流程
```
用户上传图片 → 文件验证 → 图片预处理 → 创建任务记录 → 
调用腾讯云API → 轮询任务状态 → 下载模型文件 → 生成预览图 → 
更新任务状态 → 返回结果
```

### 安全设计
- **输入验证**: 所有用户输入进行严格验证和过滤
- **文件安全**: 上传文件类型检查，病毒扫描
- **API限流**: 基于IP和用户的请求频率限制
- **数据加密**: 敏感配置信息加密存储
- **错误处理**: 统一异常处理，避免敏感信息泄露

### 性能优化
- **异步处理**: 长时间任务采用异步模式，避免阻塞
- **缓存策略**: 多层缓存设计，减少重复计算
- **连接池**: 数据库连接池优化，提升并发性能
- **文件管理**: 定期清理临时文件，避免磁盘空间不足
- **监控告警**: 实时性能监控，异常情况及时告警

## 🚀 快速开始

### 环境要求
- Java 11+
- Maven 3.6+
- MySQL 8.0+ (可选，开发环境使用H2)

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/OffNormal/qianniu-3d-generator.git
cd qianniu-3d-generator
```

2. **配置数据库**
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE qiniu_3d_generator;
```

3. **配置应用**
编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qiniu_3d_generator
    username: your_username
    password: your_password
```

4. **配置腾讯云API**
在 `application.yml` 中添加腾讯云API密钥：
```yaml
tencent:
  cloud:
    secret-id: your_secret_id
    secret-key: your_secret_key
    region: ap-beijing
```

5. **编译运行**
```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

6. **访问应用**
打开浏览器访问：http://localhost:8081

## 📖 API文档

### 基础信息
- **基础URL**: `http://localhost:8081/api/v1/ai3d`
- **数据格式**: JSON
- **字符编码**: UTF-8

### 主要接口

#### 1. 文本生成3D模型
```http
POST /api/v1/ai3d/generate/text
Content-Type: application/json

{
  "prompt": "一只可爱的小猫",
  "resultFormat": "obj",
  "enablePBR": true
}
```

#### 2. 图片生成3D模型
```http
POST /api/v1/ai3d/submit/image-url
Content-Type: application/json

{
  "imageUrl": "https://example.com/image.jpg",
  "resultFormat": "obj",
  "enablePBR": false
}
```

#### 3. 查询任务状态
```http
GET /api/v1/ai3d/query/{jobId}
```

#### 4. 下载模型文件
```http
GET /api/v1/ai3d/download/{jobId}?format=obj
```

#### 5. 历史记录管理
```http
# 获取历史记录列表
GET /api/v1/models/history?page=0&size=10&status=COMPLETED

# 获取任务详情
GET /api/v1/models/status/{taskId}

# 删除历史记录
DELETE /api/v1/models/history/{id}
```

#### 6. 管理面板接口
```http
# 获取系统统计信息
GET /api/v1/admin/stats

# 获取缓存状态
GET /api/v1/admin/cache/status
```

更多API详情请参考：[API文档](docs/api-documentation.md)

## 🎮 使用示例

### 文本生成示例
```bash
curl -X POST http://localhost:8081/api/v1/ai3d/generate/text \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "一个红色的苹果",
    "resultFormat": "obj",
    "enablePBR": true
  }'
```

### 图片上传示例
```bash
curl -X POST http://localhost:8081/api/v1/ai3d/submit/image-url \
  -H "Content-Type: application/json" \
  -d '{
    "imageUrl": "https://example.com/apple.jpg",
    "resultFormat": "stl"
  }'
```

## 📁 项目结构

```
qiniu-3d-generator/
├── docs/                                    # 项目文档
│   ├── api-documentation.md                # API接口文档
│   ├── product-requirements.md             # 产品需求文档
│   └── product-prototype.svg               # 产品原型图
├── src/main/java/com/qiniu/model3d/
│   ├── Application.java                    # Spring Boot 启动类
│   ├── config/                             # 配置类
│   │   ├── TencentCloudConfig.java         # 腾讯云服务配置
│   │   └── WebMvcConfig.java               # Web MVC 配置
│   ├── controller/                         # 控制器层
│   │   ├── ModelGenerationController.java  # 模型生成控制器
│   │   ├── Model3DHistoryController.java   # 历史记录控制器
│   │   ├── AdminDashboardController.java   # 管理面板控制器
│   │   └── FileUploadController.java       # 文件上传控制器
│   ├── service/                            # 服务层
│   │   ├── ModelGenerationService.java     # 模型生成服务
│   │   ├── CacheService.java               # 缓存服务
│   │   ├── TencentAi3dClient.java          # 腾讯云AI3D客户端
│   │   └── impl/                           # 服务实现类
│   ├── entity/                             # 实体类
│   │   ├── Model3DHistory.java             # 历史记录实体
│   │   └── GenerationTask.java             # 生成任务实体
│   ├── repository/                         # 数据访问层
│   │   ├── Model3DHistoryRepository.java   # 历史记录仓库
│   │   └── GenerationTaskRepository.java   # 任务仓库
│   ├── dto/                                # 数据传输对象
│   │   ├── GenerationRequest.java          # 生成请求DTO
│   │   └── GenerationResponse.java         # 生成响应DTO
│   └── scheduler/                          # 定时任务
│       └── TaskCleanupScheduler.java       # 任务清理调度器
├── src/main/resources/
│   ├── application.yml                     # 应用配置文件
│   └── static/                             # 静态资源
│       ├── index.html                      # 主页面
│       ├── admin/dashboard.html            # 管理面板
│       ├── css/style.css                   # 样式文件
│       └── js/app.js                       # 前端逻辑
├── src/test/                               # 测试代码
├── models/                                 # 生成的3D模型文件
├── previews/                               # 模型预览图片
├── uploads/                                # 用户上传文件
├── logs/                                   # 应用日志文件
├── pom.xml                                 # Maven项目配置
└── README.md                               # 项目说明文档
```

## 🔧 配置说明

### 应用配置
主要配置项在 `application.yml` 中：

```yaml
server:
  port: 8081                     # 服务端口

spring:
  datasource:                    # 数据库配置
    url: jdbc:mysql://localhost:3306/qiniu_3d_generator
    username: root
    password: your_password
    hikari:                      # 连接池配置
      maximum-pool-size: 20
      minimum-idle: 5
  
  servlet:
    multipart:                   # 文件上传配置
      max-file-size: 10MB
      max-request-size: 10MB
  
  jpa:                          # JPA配置
    hibernate:
      ddl-auto: update          # 开发环境使用update，生产环境使用validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true

# 应用自定义配置
app:
  file:
    upload-dir: ./uploads        # 上传文件目录
    model-dir: ./models         # 模型文件目录
    preview-dir: ./previews     # 预览图目录
    max-file-size: 10485760     # 最大文件大小(字节)
  
  model:
    max-text-length: 500        # 最大文本长度
    daily-generation-limit: 20  # 每日生成限制
    generation-timeout: 600     # 生成超时时间(秒)

# 腾讯云配置
tencent:
  cloud:
    secret-id: ${TENCENT_SECRET_ID:your_secret_id}
    secret-key: ${TENCENT_SECRET_KEY:your_secret_key}
    region: ap-guangzhou
    ai3d:
      endpoint: hunyuan.tencentcloudapi.com
      version: "2023-09-01"
      timeout: 60000
      retry-count: 3
      generation:
        complexity: medium       # 生成复杂度: simple/medium/complex
        format: jpg             # 预览格式
        style: "201"            # 艺术风格

# 日志配置
logging:
  level:
    com.qiniu.model3d: INFO
    org.springframework.web: DEBUG
  file:
    name: logs/qiniu-3d-generator.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 环境配置
支持多环境配置：
- `application-dev.yml` - 开发环境
- `application-prod.yml` - 生产环境
- `application-test.yml` - 测试环境

## 🧪 测试

### 运行单元测试
```bash
mvn test
```

### 运行集成测试
```bash
mvn verify
```

### 测试覆盖率
```bash
mvn jacoco:report
```

## 📊 性能指标

- **文本生成响应时间**: < 30秒
- **图片生成响应时间**: < 60秒
- **并发支持**: 50用户
- **文件上传限制**: 10MB
- **支持格式**: OBJ, STL, PLY

## 👥 项目分工

本项目由三位核心开发者协作完成，各自承担不同的技术领域和职责，确保项目的高质量交付。

### 🏗️ 徐同学 - 系统架构师 & 全栈开发工程师
**主要职责**: 系统整体架构设计与核心功能实现

#### 核心贡献
- **系统架构设计** 📐
  - 设计并实现三层架构模式
  - 制定技术选型方案 (Spring Boot + MySQL + 腾讯云AI)
  - 定义系统模块划分和接口规范
  - 设计数据库表结构和关系模型

- **后端系统开发** ⚙️
  - 实现完整的Spring Boot应用框架
  - 开发核心业务逻辑和服务层
  - 实现RESTful API接口设计
  - 集成腾讯云AI3D服务
  - 实现异步任务处理机制
  - 开发缓存系统和性能优化

- **前端界面开发** 🎨
  - 设计并实现用户交互界面
  - 开发响应式Web页面 (HTML/CSS/JavaScript)
  - 实现管理面板和数据可视化
  - 优化用户体验和界面美观度

- **系统集成与部署** 🚀
  - 配置多环境部署方案
  - 实现CI/CD流程
  - 系统监控和日志管理
  - 性能调优和稳定性保障

**技术栈掌握**: Java, Spring Boot, MySQL, HTML/CSS/JavaScript, 系统架构设计

### 🔗 赵同学 - API集成专家 & 测试工程师
**主要职责**: 第三方服务集成与质量保证

#### 核心贡献
- **3D模型生成服务商调研** 🔍
  - 深入调研多家3D模型生成服务提供商
  - 对比分析各服务商的API能力、性能和成本
  - 制定服务商选择标准和评估体系

- **API接口对接开发** 🔌
  - 腾讯云混元AI 3D模型生成API集成
  - 腾讯云AI艺术图像处理API对接
  - 实现API调用封装和错误处理机制
  - 开发API重试和容错策略

- **服务测试与对比** 🧪
  - 设计并执行API功能测试用例
  - 进行不同服务商的效果对比测试
  - 性能基准测试和压力测试
  - 生成详细的测试报告和优化建议

- **接口优化与监控** 📊
  - API调用性能优化
  - 实现API调用监控和告警
  - 服务可用性和稳定性保障
  - 接口文档编写和维护

**技术栈掌握**: API集成, 腾讯云服务, 接口测试, 性能优化

### 📋 蒋同学 - 产品经理 & 质量保证工程师
**主要职责**: 产品规划与质量管理

#### 核心贡献
- **产品需求分析** 📝
  - 编写详细的产品需求文档 (PRD)
  - 定义产品功能规格和用户故事
  - 制定产品发展路线图
  - 用户体验设计和交互流程规划

- **项目文档管理** 📚
  - 撰写完整的项目技术文档
  - 编写API接口文档和使用指南
  - 制作产品原型图和设计规范
  - 维护项目README和开发文档

- **数据库设计** 🗄️
  - 设计数据库表结构和关系模型
  - 定义数据字典和约束规则
  - 优化数据库查询性能
  - 数据备份和恢复策略制定

- **整体项目测试** ✅
  - 制定测试计划和测试策略
  - 执行功能测试、集成测试和用户验收测试
  - 缺陷跟踪和质量控制
  - 用户反馈收集和产品迭代

- **项目管理协调** 🎯
  - 项目进度跟踪和里程碑管理
  - 团队协作和沟通协调
  - 风险识别和应对措施
  - 项目交付和上线支持

**技术栈掌握**: 产品设计, 数据库设计, 项目管理, 质量保证, 技术文档

### 🤝 团队协作模式

#### 开发流程
1. **需求分析阶段**: 蒋同学主导需求分析和产品设计
2. **架构设计阶段**: 徐同学负责技术架构和系统设计
3. **开发实现阶段**: 徐同学主导开发，赵同学负责API集成
4. **测试验证阶段**: 赵同学和蒋同学协作进行全面测试
5. **文档完善阶段**: 蒋同学负责文档整理和项目交付

#### 技术决策
- **架构决策**: 徐同学主导，团队讨论确认
- **技术选型**: 基于调研结果，团队共同决策
- **API选择**: 赵同学调研推荐，团队评估决定
- **产品功能**: 蒋同学需求分析，团队技术评估

#### 质量保证
- **代码审查**: 徐同学主导，团队成员交叉审查
- **功能测试**: 赵同学和蒋同学分别从技术和产品角度测试
- **文档审核**: 蒋同学负责，团队成员补充完善
- **用户验收**: 蒋同学组织，团队共同参与

### 🏆 项目成果

通过团队的密切协作，我们成功交付了一个功能完整、性能稳定的3D模型生成系统：

- ✅ **技术架构**: 采用现代化的微服务架构，具备良好的扩展性和维护性
- ✅ **功能实现**: 支持文本和图片两种输入方式，生成高质量3D模型
- ✅ **性能优化**: 响应时间控制在合理范围内，支持并发访问
- ✅ **质量保证**: 经过全面测试，系统稳定可靠
- ✅ **文档完善**: 提供详细的技术文档和使用指南
- ✅ **用户体验**: 界面友好，操作简单，功能完整

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

### 代码规范
- 遵循 Java 编码规范
- 添加适当的注释和文档
- 编写单元测试
- 确保代码通过所有测试

## 📝 更新日志

### v1.0.0 (2024-01-20)
- ✨ 初始版本发布
- 🎯 支持文本生成3D模型
- 🖼️ 支持图片生成3D模型
- 📁 支持多种3D格式导出
- 🔄 异步任务处理机制
- 📊 完整的API接口

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [腾讯云AI服务](https://cloud.tencent.com/product/ai) - 提供强大的AI能力
- [Spring Boot](https://spring.io/projects/spring-boot) - 优秀的Java框架
- [Maven](https://maven.apache.org/) - 项目构建工具

## 📞 联系我们

- 项目主页: https://github.com/OffNormal/qianniu-3d-generator
- 问题反馈: https://github.com/OffNormal/qianniu-3d-generator/issues
- 邮箱: xhs2023@outlook.com

---

⭐ 如果这个项目对你有帮助，请给我们一个星标！
