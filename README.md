# 指标管理系统

## 版本信息
**v0.01** - 添加主题管理和简单账号管理功能

## 项目简介
这是一个基于 Spring Boot + PostgreSQL 的指标管理系统，允许业务分析人员在前端配置和管理数据分析指标。

## 功能特性
### v0.01 新增功能
- ✅ **主题管理**：指标归属于特定主题，支持主题分类管理
- ✅ **权限控制**：三级权限体系（超级管理员、主题管理员、普通用户）
- ✅ **用户管理**：支持用户增删改查和角色分配
- ✅ **登录认证**：基于 Spring Security 的会话认证
- ✅ **主题分配**：管理员可为主题分配成员和管理员

### 核心功能
- ✅ 支持统计型和明细型两种指标类型
- ✅ 配置主表、关联表、关联字段
- ✅ 支持维度字段和过滤字段配置
- ✅ 指标注释/描述管理
- ✅ 指标的增删改查功能
- ✅ 搜索和筛选功能
- ✅ 美观的前端界面

## 技术栈
- **后端**: Spring Boot 3.2.0 + JPA
- **数据库**: PostgreSQL 16
- **前端**: HTML5 + CSS3 + JavaScript (原生)
- **构建工具**: Maven

## 数据库配置

### 连接信息
- **主机**: 192.168.*.*
- **端口**: 5432
- **数据库**: vectordb
- **用户名**: root
- **密码**: root

### 建表
在 PostgreSQL 中执行以下 SQL 脚本创建表：
```bash
psql -U root -d vectordb -f src/main/resources/schema.sql
```

或者手动执行 `src/main/resources/schema.sql` 中的 SQL 语句。

## 快速开始

### 1. 数据库准备
在 PostgreSQL 中执行以下 SQL 脚本创建表：
```bash
psql -U root -d vectordb -f src/main/resources/schema_with_auth.sql
```

### 2. 编译项目
```bash
mvn clean package
```

### 3. 运行项目
```bash
mvn spring-boot:run
```

或者直接运行 Main 类。

### 4. 初始化默认用户
应用启动后，执行以下命令初始化默认用户：
```bash
curl -X POST http://localhost:8080/init/users
```

### 5. 访问系统
打开浏览器访问：http://localhost:8080

## 权限体系

### 角色说明
| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 超级管理员 | root | root | 所有权限，可管理用户和主题 |
| 主题管理员 | admin1/admin2 | admin1/admin2 | 可管理主题下的指标和成员 |
| 普通用户 | user1/user2 | user1/user2 | 只有查询权限 |

### 权限规则
- **超级管理员**：可以创建/编辑/删除用户，创建/编辑/删除主题，分配主题管理员
- **主题管理员**：可以创建/编辑/删除指标，分配主题成员
- **普通用户**：只能查看指标，不能编辑

## 使用说明

### 新增指标
1. 填写指标名称（必填）
2. 选择指标类型：统计 或 明细（必填）
3. 输入主表名（必填）
4. 添加关联表（可选）：输入表名和别名，点击"添加"
5. 添加关联字段（可选）：输入主表字段和关联表字段，点击"添加"
6. 输入维度字段（可选）：多个字段用逗号分隔
7. 添加过滤字段（可选）：输入字段名、选择操作符、输入值，点击"添加"
8. 输入指标注释（可选）
9. 点击"保存"按钮

### 编辑指标
1. 在指标列表中找到要编辑的指标
2. 点击"编辑"按钮
3. 修改相关信息
4. 点击"保存"按钮

### 删除指标
1. 在指标列表中找到要删除的指标
2. 点击"删除"按钮
3. 确认删除

### 搜索指标
1. 在搜索框中输入关键词
2. 点击"搜索"按钮
3. 支持按指标名称或描述搜索

## API 接口

### 基础路径
`http://localhost:8080/api/metrics`

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/metrics | 创建指标 |
| PUT | /api/metrics/{id} | 更新指标 |
| DELETE | /api/metrics/{id} | 删除指标 |
| GET | /api/metrics | 获取所有指标 |
| GET | /api/metrics/{id} | 根据ID获取指标 |
| GET | /api/metrics/type/{type} | 根据类型获取指标 |
| GET | /api/metrics/search?keyword=xxx | 搜索指标 |

### 请求示例

#### 创建指标
``json
POST http://localhost:8080/api/metrics
Content-Type: application/json

{
  "metricName": "用户注册统计",
  "metricType": "STATISTICS",
  "mainTable": "users",
  "joinTables": "[{\"table\":\"orders\",\"alias\":\"o\"}]",
  "joinFields": "[{\"mainField\":\"user_id\",\"joinField\":\"user_id\"}]",
  "dimensionFields": "register_date,city",
  "filterFields": "[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"active\"}]",
  "description": "统计每日各城市活跃用户的注册数量"
}
```

## 数据模型

### MetricDefinition 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键ID |
| metric_name | VARCHAR(200) | 指标名称 |
| metric_type | VARCHAR(50) | 指标类型（STATISTICS/DETAIL） |
| main_table | VARCHAR(200) | 主表名 |
| join_tables | TEXT | 关联表（JSON格式） |
| join_fields | TEXT | 关联字段（JSON格式） |
| dimension_fields | TEXT | 维度字段（逗号分隔） |
| filter_fields | TEXT | 过滤字段（JSON格式） |
| description | TEXT | 指标注释 |
| created_by | VARCHAR(100) | 创建人 |
| created_time | TIMESTAMP | 创建时间 |
| updated_by | VARCHAR(100) | 更新人 |
| updated_time | TIMESTAMP | 更新时间 |
| is_deleted | BOOLEAN | 是否删除（逻辑删除） |

## JSON 字段格式说明

### join_tables（关联表）
```json
[
  {"table": "orders", "alias": "o"},
  {"table": "products", "alias": "p"}
]
```

### join_fields（关联字段）
```json
[
  {"mainField": "user_id", "joinField": "id"},
  {"mainField": "product_id", "joinField": "id"}
]
```

### filter_fields（过滤字段）
```json
[
  {"field": "status", "operator": "=", "value": "active"},
  {"field": "amount", "operator": ">", "value": "100"}
]
```

## 注意事项

1. 确保 PostgreSQL 数据库已启动并可访问
2. 首次运行前需要执行建表 SQL 脚本
3. 指标名称不能重复
4. 删除操作为逻辑删除，不会真正从数据库中删除数据
5. 前端页面已配置跨域访问，可以直接打开 HTML 文件使用

## 项目结构

```
sqldm/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── Main.java                    # 启动类
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java      # 安全配置
│   │   │   ├── entity/
│   │   │   │   ├── MetricDefinition.java    # 指标实体
│   │   │   │   ├── SysUser.java             # 用户实体
│   │   │   │   ├── Topic.java               # 主题实体
│   │   │   │   └── UserTopic.java           # 用户主题关联实体
│   │   │   ├── repository/
│   │   │   │   ├── MetricRepository.java    # 指标数据访问层
│   │   │   │   ├── SysUserRepository.java   # 用户数据访问层
│   │   │   │   ├── TopicRepository.java     # 主题数据访问层
│   │   │   │   └── UserTopicRepository.java # 用户主题关联数据访问层
│   │   │   ├── service/
│   │   │   │   ├── MetricService.java       # 指标业务逻辑层
│   │   │   │   ├── UserService.java         # 用户业务逻辑层
│   │   │   │   └── TopicService.java        # 主题业务逻辑层
│   │   │   └── controller/
│   │   │       ├── MetricController.java    # 指标控制器
│   │   │       ├── UserController.java      # 用户控制器
│   │   │       ├── TopicController.java     # 主题控制器
│   │   │       ├── AuthController.java      # 认证控制器
│   │   │       └── InitController.java      # 初始化控制器
│   │   └── resources/
│   │       ├── application.yml              # 配置文件
│   │       ├── schema_with_auth.sql         # 建表脚本（含权限表）
│   │       └── static/
│   │           ├── index.html               # 指标管理页面
│   │           ├── login.html               # 登录页面
│   │           └── admin.html               # 系统管理页面
│   └── test/java/
└── pom.xml
```

## 常见问题

### 1. 数据库连接失败
检查 PostgreSQL 服务是否启动，以及连接配置是否正确。

### 2. 端口被占用
修改 `application.yml` 中的 `server.port` 配置。

### 3. 表不存在
确保已执行 `schema.sql` 建表脚本。

## 开发者信息
- 开发时间：2026年6月
- 技术支持：Qoder AI Assistant
