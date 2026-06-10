# 指标管理系统

## 版本信息
**v0.03** - 指标增加数据源、SQL 模版与参数定义，完善文档

### v0.03 变更说明（2026-06-10）

#### 指标管理
- 新增**数据源**字段，标识指标查询所用的数据源
- 新增 **SQL 模版**字段，支持 `${paramName}` 参数占位符
- 新增**参数定义**字段，JSON 格式描述 SQL 参数
- 草稿状态下数据源和 SQL 模版可暂空，启用/停用状态下必填
- 列表增加数据源列，搜索支持按数据源检索

#### 文档
- 更新 README、QUICKSTART、AUTH_GUIDE 与当前功能对齐

**v0.02** - 重构指标与主题数据模型

### v0.02 变更说明（2026-06-10）

#### 主题管理
- 前端移除「主题代码」字段，仅需维护**主题名称**和**描述**
- 主题名称唯一性校验；`topic_code` 由后端自动生成，不对用户暴露

#### 指标管理（数据模型重构）
指标字段调整为面向业务的定义：

| 字段 | 说明 | 必填 |
|------|------|------|
| 指标名称 | 唯一 | 是 |
| 指标编码 | 唯一 | 是 |
| 业务口径 | 指标业务统计说明 | 是 |
| 统计周期 | 日 / 月 / 周 / 实时 | 否 |
| 所属主题域 | 关联主题 | 是 |
| 负责人 | 指标负责人 | 是 |
| 状态 | 草稿 / 启用 / 停用 | 是 |

#### 已移除的旧功能
- 指标类型（统计/明细）、主表、关联表、维度字段、过滤字段等 SQL 构建字段
- SQL 解析、SQL 生成相关接口与前端功能

**v0.01** - 添加主题管理和简单账号管理功能

## 项目简介
这是一个基于 Spring Boot + PostgreSQL 的指标管理系统，允许业务分析人员在前端配置和管理数据分析指标。

## 功能特性
### v0.03 新增功能
- ✅ **数据源配置**：为指标指定查询数据源
- ✅ **SQL 模版**：支持参数化 SQL 模版（`${paramName}`）
- ✅ **参数定义**：JSON 格式声明 SQL 参数类型与说明

### v0.01 新增功能
- ✅ **主题管理**：指标归属于特定主题，支持主题分类管理
- ✅ **权限控制**：三级权限体系（超级管理员、主题管理员、普通用户）
- ✅ **用户管理**：支持用户增删改查和角色分配
- ✅ **登录认证**：基于 Spring Security 的会话认证
- ✅ **主题分配**：管理员可为主题分配成员和管理员

### 核心功能
- ✅ 指标完整业务定义（名称、编码、业务口径、统计周期、主题域、负责人、状态）
- ✅ SQL 模版与参数定义配置
- ✅ 数据源标识配置
- ✅ 指标的增删改查功能
- ✅ 按主题域筛选、关键词搜索
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

### 主题管理（admin.html）
1. 填写主题名称（必填，唯一）
2. 填写主题描述（可选）
3. 点击「保存」
4. 支持编辑、删除、为用户分配主题成员/管理员

### 新增指标
1. 填写指标名称、指标编码（均必填且唯一）
2. 填写业务口径（必填）
3. 选择所属主题域（必填）
4. 选择统计周期（可选：日 / 月 / 周 / 实时）
5. 填写负责人（必填）
6. 选择状态（草稿 / 启用 / 停用）
7. 填写数据源（启用/停用状态下必填，如 `vectordb`）
8. 填写 SQL 模版（启用/停用状态下必填，可使用 `${paramName}` 占位符）
9. 填写参数定义（可选，JSON 格式）
10. 点击「保存」按钮

> **说明**：状态为「草稿」时，数据源和 SQL 模版可暂空，便于分步保存。

### SQL 模版示例
```sql
SELECT city, COUNT(*) AS cnt
FROM orders
WHERE order_date >= ${startDate}
GROUP BY city
```

### 参数定义示例
```json
[
  {"name": "startDate", "type": "date", "required": true, "description": "开始日期"},
  {"name": "city", "type": "string", "required": false, "description": "城市"}
]
```

### 编辑指标
1. 在指标列表中找到要编辑的指标
2. 点击「编辑」按钮
3. 修改相关信息
4. 点击「保存」按钮

### 删除指标
1. 在指标列表中找到要删除的指标
2. 点击「删除」按钮
3. 确认删除

### 搜索指标
1. 在搜索框中输入关键词
2. 点击「搜索」按钮
3. 支持按指标名称、编码、数据源、业务口径、负责人搜索
4. 导航栏可按主题域筛选指标

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
| GET | /api/metrics/search?keyword=xxx | 搜索指标 |
| GET | /api/metrics/topic/{topicId} | 按主题域获取指标 |

### 请求示例

#### 创建指标
```json
POST http://localhost:8080/api/metrics
Content-Type: application/json

{
  "metricName": "订单量统计",
  "metricCode": "order_cnt_daily",
  "businessCaliber": "统计每日各城市的有效订单数量",
  "statPeriod": "DAY",
  "topicId": 1,
  "owner": "张三",
  "status": "ACTIVE",
  "dataSource": "vectordb",
  "sqlTemplate": "SELECT city, COUNT(*) AS cnt FROM orders WHERE order_date >= ${startDate} GROUP BY city",
  "paramDefinition": "[{\"name\":\"startDate\",\"type\":\"date\",\"required\":true,\"description\":\"开始日期\"}]"
}
```

## 数据模型

### MetricDefinition 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 ID |
| metric_name | VARCHAR(200) | 指标名称（唯一） |
| metric_code | VARCHAR(100) | 指标编码（唯一） |
| business_caliber | TEXT | 业务口径 |
| stat_period | VARCHAR(50) | 统计周期（DAY/MONTH/WEEK/REALTIME） |
| topic_id | BIGINT | 所属主题域 ID |
| topic_name | VARCHAR(200) | 所属主题域名称（冗余） |
| owner | VARCHAR(100) | 负责人 |
| status | VARCHAR(50) | 状态（DRAFT/ACTIVE/DISABLED） |
| data_source | VARCHAR(200) | 数据源 |
| sql_template | TEXT | SQL 模版 |
| param_definition | TEXT | 参数定义（JSON） |
| created_by | VARCHAR(100) | 创建人 |
| created_time | TIMESTAMP | 创建时间 |
| updated_by | VARCHAR(100) | 更新人 |
| updated_time | TIMESTAMP | 更新时间 |
| is_deleted | BOOLEAN | 是否删除（逻辑删除） |

### Topic 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | SERIAL | 主键 ID |
| topic_name | VARCHAR(200) | 主题名称（唯一） |
| topic_code | VARCHAR(100) | 主题代码（后端自动生成，不对用户暴露） |
| description | TEXT | 主题描述 |
| admin_id | BIGINT | 主题管理员 ID |
| created_time | TIMESTAMP | 创建时间 |
| updated_time | TIMESTAMP | 更新时间 |
| is_deleted | BOOLEAN | 是否删除 |

## JSON 字段格式说明

### param_definition（参数定义）
```json
[
  {"name": "startDate", "type": "date", "required": true, "description": "开始日期"},
  {"name": "city", "type": "string", "required": false, "description": "城市"}
]
```

### 统计周期枚举（stat_period）

| 值 | 含义 |
|----|------|
| DAY | 日 |
| MONTH | 月 |
| WEEK | 周 |
| REALTIME | 实时 |

### 状态枚举（status）

| 值 | 含义 |
|----|------|
| DRAFT | 草稿 |
| ACTIVE | 启用 |
| DISABLED | 停用 |

## 注意事项

1. 确保 PostgreSQL 数据库已启动并可访问
2. 首次运行前需要执行建表 SQL 脚本（推荐 `schema_with_auth.sql`）
3. 应用使用 `ddl-auto: update`，新增字段会自动同步到数据库
4. 指标名称、指标编码均不能重复
5. 主题名称不能重复
6. 删除操作为逻辑删除，不会真正从数据库中删除数据
7. 前端页面已配置跨域访问，可以直接打开 HTML 文件使用
8. 旧版指标数据需补全新字段（编码、业务口径、负责人、数据源、SQL 模版等）后方可正常编辑保存

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
