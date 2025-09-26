# 七牛3D模型生成器 🎨

[![Java](https://img.shields.io/badge/Java-11-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.15-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

一个基于腾讯混元AI的轻量级3D模型生成器，支持通过文本描述或图片上传生成高质量3D模型。

## ✨ 功能特性

### 🎯 核心功能
- **文本生成3D模型** - 通过自然语言描述生成3D模型
- **图片生成3D模型** - 上传图片自动识别并生成对应3D模型
- **多格式支持** - 支持OBJ、STL、PLY等主流3D格式
- **实时预览** - 在线3D模型预览和交互

### 🛠️ 辅助功能
- **任务管理** - 异步任务处理，支持任务状态查询
- **文件下载** - 支持生成模型的批量下载
- **参数配置** - 可调节模型复杂度、格式等参数
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

## 🚀 快速开始

### 环境要求
- Java 11+
- Maven 3.6+
- MySQL 8.0+ (可选，开发环境使用H2)

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/your-username/qiniu-3d-generator.git
cd qiniu-3d-generator
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
├── docs/                          # 项目文档
│   ├── api-documentation.md       # API接口文档
│   ├── product-requirements.md    # 产品需求文档
│   └── product-prototype.svg      # 产品原型图
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/qiniu/model3d/
│   │   │       ├── controller/    # 控制器层
│   │   │       ├── service/       # 服务层
│   │   │       ├── dto/          # 数据传输对象
│   │   │       └── config/       # 配置类
│   │   └── resources/
│   │       ├── application.yml    # 应用配置
│   │       └── static/           # 静态资源
│   └── test/                     # 测试代码
├── uploads/                      # 文件上传目录
├── logs/                        # 日志文件
├── pom.xml                      # Maven配置
└── README.md                    # 项目说明
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
  
  servlet:
    multipart:                   # 文件上传配置
      max-file-size: 10MB
      max-request-size: 10MB

tencent:                         # 腾讯云配置
  cloud:
    secret-id: your_secret_id
    secret-key: your_secret_key
    region: ap-beijing
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

- 项目主页: https://github.com/your-username/qiniu-3d-generator
- 问题反馈: https://github.com/your-username/qiniu-3d-generator/issues
- 邮箱: your-email@example.com

---

⭐ 如果这个项目对你有帮助，请给我们一个星标！
