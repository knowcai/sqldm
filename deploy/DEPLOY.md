# SQLDM 部署说明

应用与数据库分离部署：**数据库连接通过 `config.env` 配置**，**SQL 初始化脚本由用户手工执行**，应用不会自动建表。

---

## 1. 环境要求

| 组件 | 版本 |
|------|------|
| JDK / JRE | 21 |
| PostgreSQL | 14+（推荐 16） |
| 操作系统 | Linux / Windows |

---

## 2. 部署步骤

### 2.1 准备外部数据库

1. 在 PostgreSQL 中创建目标库（例如 `vectordb`）
2. 确保应用服务器能访问数据库主机和端口（默认 5432）

### 2.2 手工执行 SQL 初始化（必做，仅首次或重置时）

应用启动前，须由 DBA 或运维**手工**执行初始化脚本：

```
src/main/resources/sql/sqldm_v1.0_full_init.sql
```

**Linux / macOS（psql）：**

```bash
psql -h <数据库主机> -U <用户名> -d <库名> -f src/main/resources/sql/sqldm_v1.0_full_init.sql
```

**Windows（psql）：**

```powershell
psql -h <数据库主机> -U <用户名> -d <库名> -f src\main\resources\sql\sqldm_v1.0_full_init.sql
```

如需清空重建（会删除 public schema 下所有对象）：

```bash
psql -h <主机> -U <用户> -d <库名> -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO public;"
psql -h <主机> -U <用户> -d <库名> -f src/main/resources/sql/sqldm_v1.0_full_init.sql
```

初始化后默认账号：`root` / `root`（超级管理员）

> 部署脚本**不会**自动执行 SQL，请在上一步确认表结构已就绪后再启动应用。

### 2.3 编译应用

```bash
mvn clean package -DskipTests
```

产物：`target/sqldm-1.0-SNAPSHOT.jar`

### 2.4 配置数据库连接

```bash
cd deploy
cp config.env.example config.env    # Windows: Copy-Item config.env.example config.env
```

编辑 `config.env`：

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://192.168.31.100:5432/vectordb
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password
SERVER_PORT=8080
```

### 2.5 启动 / 停止

**Linux：**

```bash
chmod +x deploy/start.sh deploy/stop.sh
./deploy/start.sh
./deploy/stop.sh
```

**Windows：**

```powershell
.\deploy\start.ps1
.\deploy\stop.ps1
```

启动后访问：

| 页面 | URL |
|------|-----|
| 登录 | http://localhost:8080/login.html |
| 健康检查 | http://localhost:8080/actuator/health |

日志：`deploy/logs/app.log`

---

## 3. 生产发布包（可选）

将以下内容打包分发到目标服务器：

```
sqldm-deploy/
├── sqldm.jar              # 从 target/ 复制并重命名
├── config.env.example
├── start.sh / start.ps1
├── stop.sh  / stop.ps1
└── sql/
    └── sqldm_v1.0_full_init.sql   # 供 DBA 手工执行，应用不会自动运行
```

目标服务器操作流程：

1. DBA 手工执行 `sql/sqldm_v1.0_full_init.sql`
2. 运维复制 `config.env.example` → `config.env` 并填写连接
3. 运行 `start.sh` 或 `start.ps1`

---

## 4. Docker 部署（仅应用，外部数据库）

```bash
docker build -t sqldm:1.0.0 .

docker run -d --name sqldm \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://192.168.31.100:5432/vectordb \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  sqldm:1.0.0
```

或使用 `docker-compose.yml`（需先在 `.env` 中配置数据库连接）。

**注意：** Docker 同样不会自动初始化数据库，须事先手工执行 SQL 脚本。

---

## 5. 常见问题

| 现象 | 处理 |
|------|------|
| 启动报表不存在 / validate 失败 | 未手工执行 init SQL |
| 登录用户不存在 | 确认 SQL 已执行且 `config.env` 连接的是正确库 |
| 端口占用 | 修改 `config.env` 中 `SERVER_PORT` |
| 连接数据库失败 | 检查网络、防火墙、用户名密码 |
