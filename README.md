# 指标管理系统

## 版本信息
**v0.05** - 指标目录、Open API 治理、审批流、工程化底座（不含 SQL 试跑/数据源连接管理）

### v0.05 变更说明（2026-06-12）

#### 审批流增强
- **主题审批员**（UserTopic.ADMIN）审批所负责主题；系统角色 TOPIC_ADMIN ≠ 审批权限（UI 有说明）
- 审批弹窗展示完整指标详情与变更 diff
- **我的申请**：提交人查看待审/驳回项，驳回后可编辑重新提交
- Webhook 新增事件：`METRIC_SUBMIT`、`METRIC_APPROVED`、`METRIC_REJECTED`
- 删除权限与审批权限对齐（主题审批员 / 超级管理员）

#### 列表与发现
- 搜索、主题、状态、标签、排序 **统一** 于 `GET /api/metrics`
- 列表星标收藏、「变更审」「可重提」状态提示
- 详情 SQL 等宽展示；Open API curl 调用示例
- 前端 API 地址使用 `window.location.origin`

#### Open API 治理
- 调用日志统计：总量、失败率、热门指标/Key（`/api/access-logs/stats`）

#### 规则与工程
- 指标名称 **主题内唯一**，编码 **全局唯一**
- JPA `ddl-auto: validate`，结构变更仅通过 Flyway
- 单元测试：编码校验、IP 白名单、审批字段 Diff（MetricDiffHelperTest）；API 自测脚本 `scripts/self-test.ps1`

#### 指标目录（方案 1）
- **标签体系**：指标可打多个标签，列表支持按标签筛选
- **详情增强**：弹窗展示主题、更新时间、标签；支持收藏（列表星标）
- **参数 Schema**：Open API 返回完整参数结构（type/required/description）
- **命名规范**：指标编码默认校验大写字母+数字+下划线（可配置关闭）

#### Open API 治理（方案 2）
- **调用日志**：记录 Open API 每次访问（IP、指标、成功/失败、耗时）
- **Swagger 文档**：`/swagger-ui.html`
- **按版本查询**：`GET /api/open/metrics?code=xxx&version=2`
- Open API 响应增加口径、周期、主题、标签、版本号

#### 协作与治理（方案 3）
- **重复检测**：同主题下同名/同编码/名称相似时保存提示 warnings
- **Webhook**：指标创建/更新/状态变更/删除时异步通知（系统管理配置）
- **审批流**（v0.06 完善）：普通用户提交新增/变更，主题审批员按主题维度审批

#### 工程化（方案 4）
- **Flyway** 数据库迁移（`src/main/resources/db/migration/`）
- **Docker Compose**：`docker-compose.yml` 一键启动
- **Actuator**：`/actuator/health`
- **单元测试**：编码校验、IP 白名单
- **application-prod.yml**：生产配置外置模板

#### 定位说明
- `dataSource` 为**元数据标识**（告诉下游用哪个库/仓），本系统**不管理 JDBC 连接、不提供 SQL 试跑**

**v0.04** - 对外 Open API、前端交互与暗色主题优化

### v0.04 变更说明（2026-06-11）

#### 对外 Open API
- 新增独立模块 `org.example.openapi`，路径 `/api/open/metrics`
- 供外部系统查询**已启用（ACTIVE）**指标的 SQL 模版、数据源与参数定义
- **无需登录**即可访问；草稿/停用指标不会返回

#### 前端优化
- 指标新增/编辑改为**弹窗**操作，列表页增加「新增指标」按钮
- 参数定义改为可视化编辑（参数名 + 说明，手动添加）
- 全站切换为**黑色暗色主题**（`theme.css`）

#### 数据库
- 应用启动时自动执行 `DatabaseMigration`，修复旧版 `metric_definition` 表 NOT NULL 约束问题

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
### v0.04 新增功能
- ✅ **对外 Open API**：外部系统按指标编码查询 SQL 模版与参数（仅返回已启用指标）
- ✅ **弹窗式指标编辑**：列表页新增/编辑指标均在弹窗中完成
- ✅ **可视化参数定义**：手动维护参数名与说明
- ✅ **暗色主题**：统一黑色酷感 UI（`theme.css`）

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
- 登录页：http://localhost:8080/login.html
- 指标管理：http://localhost:8080
- 系统管理：http://localhost:8080/admin.html

默认账号：`root` / `root`

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
1. 在指标列表页点击「**新增指标**」按钮，打开弹窗
2. 填写指标名称、指标编码（均必填且唯一）
3. 填写业务口径（必填）
4. 选择所属主题域（必填）
5. 选择统计周期（可选：日 / 月 / 周 / 实时）
6. 填写负责人（必填）
7. 选择状态（草稿 / 启用 / 停用）
8. 填写数据源（启用/停用状态下必填，如 `vectordb`）
9. 填写 SQL 模版（启用/停用状态下必填，可使用 `${paramName}` 占位符）
10. 在「参数定义」中点击「添加参数」，填写参数名与说明
11. 点击「保存」按钮

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
2. 点击「编辑」按钮，在弹窗中修改信息
3. 点击「保存」按钮

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

### 一、内部管理 API（需登录）

#### 基础路径
`http://localhost:8080/api/metrics`

#### 接口列表

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | /api/metrics | 创建指标 | 需登录 |
| PUT | /api/metrics/{id} | 更新指标 | 需登录 |
| DELETE | /api/metrics/{id} | 删除指标 | 需登录 |
| GET | /api/metrics | 获取所有指标 | 需登录 |
| GET | /api/metrics/{id} | 根据ID获取指标 | 需登录 |
| GET | /api/metrics/search?keyword=xxx | 搜索指标 | 需登录 |
| GET | /api/metrics/topic/{topicId} | 按主题域获取指标 | 需登录 |

#### 创建指标示例

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

---

### 二、对外 Open API（无需登录）

供其他业务系统读取**已启用**指标的 SQL 模版与参数定义。

#### 基础路径
`http://localhost:8080/api/open/metrics`

#### 访问规则
- **仅返回状态为 `ACTIVE`（启用）的指标**
- 草稿（`DRAFT`）、停用（`DISABLED`）指标不会出现在结果中
- 按编码/ID 查询非启用指标时，返回 `success: false`

#### 接口列表

| 场景 | 方法 | 请求 | 说明 |
|------|------|------|------|
| 查询全部已启用指标 | GET | `/api/open/metrics` | 返回列表 |
| 按指标编码查询 | GET | `/api/open/metrics?code={metricCode}` | 返回单个指标 |
| 按指标 ID 查询 | GET | `/api/open/metrics?id={id}` | 返回单个指标 |

#### 请求示例

```bash
# 1. 查询全部已启用指标
curl "http://localhost:8080/api/open/metrics"

# 2. 按指标编码查询（推荐外部系统使用）
curl "http://localhost:8080/api/open/metrics?code=order_cnt_daily"

# 3. 按指标 ID 查询
curl "http://localhost:8080/api/open/metrics?id=1"
```

#### 成功响应示例（按编码查询）

```json
{
  "success": true,
  "data": {
    "metricCode": "order_cnt_daily",
    "metricName": "订单量统计",
    "dataSource": "vectordb",
    "sqlTemplate": "SELECT city, COUNT(*) AS cnt FROM orders WHERE order_date >= ${startDate} GROUP BY city",
    "params": [
      {
        "name": "startDate",
        "type": "date",
        "required": true,
        "description": "开始日期"
      }
    ]
  }
}
```

#### 列表响应示例

```json
{
  "success": true,
  "total": 2,
  "data": [
    {
      "metricCode": "order_cnt_daily",
      "metricName": "订单量统计",
      "dataSource": "vectordb",
      "sqlTemplate": "SELECT ...",
      "params": []
    }
  ]
}
```

#### 失败响应示例

```json
{
  "success": false,
  "message": "指标不存在或未启用: NOT_EXIST"
}
```

#### 返回字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| metricCode | String | 指标编码（外部系统主键推荐用这个） |
| metricName | String | 指标名称 |
| dataSource | String | 数据源标识 |
| sqlTemplate | String | SQL 模版，含 `${paramName}` 占位符 |
| params | Array | 参数列表 |
| params[].name | String | 参数名 |
| params[].type | String | 参数类型（如 string、date） |
| params[].required | Boolean | 是否必填 |
| params[].description | String | 参数说明 |

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
7. 前端请通过 Spring Boot 服务访问（http://localhost:8080），不要直接双击打开 HTML 文件
8. 旧版指标数据需补全新字段（编码、业务口径、负责人、数据源、SQL 模版等）后方可正常编辑保存
9. 若从 v0.01 升级，应用启动时会自动迁移 `metric_definition` 表结构；也可手动执行 `migration_v003.sql`

## 项目结构

```
sqldm/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── Main.java                    # 启动类
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java      # 安全配置
│   │   │   │   └── DatabaseMigration.java   # 启动时数据库迁移
│   │   │   ├── openapi/                     # 对外 Open API 模块
│   │   │   │   ├── controller/MetricOpenApiController.java
│   │   │   │   ├── service/MetricOpenApiService.java
│   │   │   │   └── dto/                     # 对外返回 DTO
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
│   │       ├── migration_v003.sql           # v0.03 表结构迁移脚本
│   │       └── static/
│   │           ├── theme.css                # 暗色主题样式
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
确保已执行 `schema_with_auth.sql` 建表脚本。

### 4. 保存指标报错（main_table NOT NULL）
旧版数据库需迁移，重启应用后会自动执行；或手动执行 `migration_v003.sql`。

### 5. GitHub 连接失败
见下方「GitHub 连接问题排查」。

## GitHub 连接问题排查

若 `git push` / `git fetch` 报错 `Failed to connect to github.com port 443`，可按以下步骤排查：

1. **检查 Git 代理配置**
   ```bash
   git config --global --get http.proxy
   git config --global --get https.proxy
   ```
   若配置了 `http://127.0.0.1:7890` 等本地代理，需确保代理软件（Clash、V2Ray 等）**已启动**且端口一致。

2. **代理未运行时**
   - 启动代理软件后再执行 `git fetch origin`
   - 或临时取消代理：
     ```bash
     git config --global --unset http.proxy
     git config --global --unset https.proxy
     ```

3. **直连测试**
   ```bash
   curl -I --connect-timeout 10 https://github.com
   ```
   若超时，说明当前网络无法直连 GitHub，必须使用代理或更换网络。

4. **本仓库远程地址**
   ```bash
   git remote -v
   ```
   当前远程：`https://github.com/knowcai/sqldm.git`

## 开发者信息
- 开发时间：2026年6月
- 技术支持：Qoder AI Assistant
