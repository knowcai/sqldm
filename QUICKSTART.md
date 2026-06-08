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

1. 在表单中填写指标信息
2. 点击"保存"按钮
3. 在下方列表中查看已创建的指标
4. 可以编辑、删除或搜索指标

## 示例数据

系统已内置两个示例指标：
- 用户注册统计（统计型）
- 订单明细查询（明细型）

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

- 查看 README.md 了解详细功能说明
- 查看 API 文档了解接口使用方法
- 根据实际需求修改和扩展系统功能
