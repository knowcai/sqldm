# 快速启动指南

## 第一步：创建数据库表

在命令行中执行：

```bash
psql -U root -d vectordb -h 192.168.31.100 -f src/main/resources/schema.sql
```

或者连接到数据库后手动执行 schema.sql 中的内容。

## 第二步：启动应用

### 方式一：使用 Maven（推荐）
```bash
mvn spring-boot:run
```

### 方式二：先编译再运行
```bash
mvn clean package
java -jar target/sqldm-1.0-SNAPSHOT.jar
```

### 方式三：在 IDE 中运行
直接运行 `org.example.Main` 类

## 第三步：访问系统

打开浏览器访问：http://localhost:8080

## 第四步：开始使用

1. 使用 `root/root` 登录系统
2. 在「系统管理」中创建主题域（仅需名称和描述）
3. 在指标管理页填写指标信息（名称、编码、业务口径、主题域、负责人、数据源、SQL 模版等）
4. 点击「保存」按钮
5. 在下方列表中查看、编辑、删除或搜索指标

## 指标字段速查

| 字段 | 必填 | 说明 |
|------|------|------|
| 指标名称 | 是 | 唯一 |
| 指标编码 | 是 | 唯一 |
| 业务口径 | 是 | 业务统计说明 |
| 所属主题域 | 是 | 从主题列表选择 |
| 负责人 | 是 | 指标负责人 |
| 状态 | 是 | 草稿 / 启用 / 停用 |
| 统计周期 | 否 | 日 / 月 / 周 / 实时 |
| 数据源 | 启用时必填 | 如 `vectordb` |
| SQL 模版 | 启用时必填 | 支持 `${paramName}` |
| 参数定义 | 否 | JSON 格式 |

## 验证数据库连接

如果启动失败，可以先测试数据库连接：

```bash
psql -U root -d vectordb -h 192.168.31.100
```

输入密码 `root` 后，如果能成功连接，说明配置正确。

## 常见问题排查

### 问题1：无法连接数据库
- 检查 PostgreSQL 服务是否启动
- 检查防火墙是否允许访问 192.168.31.100:5432
- 确认用户名密码是否正确

### 问题2：端口 8080 被占用
修改 `src/main/resources/application.yml` 中的端口：
```yaml
server:
  port: 8081  # 改为其他端口
```

### 问题3：表不存在
确保已执行建表脚本：
```sql
\c vectordb
\i src/main/resources/schema.sql
```

## 下一步

- 查看 README.md 了解详细功能说明与 v0.03 变更记录
- 查看 AUTH_GUIDE.md 了解权限与主题管理
- 根据实际需求修改和扩展系统功能
