# MySQL 5.6 / 5.7 兼容性测试指南

本地 MySQL 8.x/9.x 与外部 MySQL 5.6/5.7 的兼容性测试说明。

## 一、快速测试（连接外部 MySQL 5.7）

### 1. 设置环境变量

```bat
set MYSQL_EXTERNAL=你的外部数据库IP:3306
set MYSQL_USER=root
set MYSQL_PASS=你的密码
```

### 2. 运行兼容性测试

```bat
cd D:\小程序\backend\database
test_mysql57_compatibility.bat
```

脚本会：
- 连接目标数据库
- 执行 `test_mysql57_compatibility.sql`
- 检查版本、字符集、建表语法等
- 输出是否通过

### 3. 手动执行（无 mysql 命令行时）

在 MySQL 客户端（如 Navicat、DBeaver、phpMyAdmin）中：

1. 连接到你的**外部 MySQL 5.6/5.7**
2. 打开并执行 `test_mysql57_compatibility.sql`
3. 查看输出：`mysql_version` 应为 5.6.x 或 5.7.x，且无报错即表示通过

---

## 二、迁移到外部 MySQL 5.7

### 1. 执行顺序

若外部数据库是**全新**的，按以下顺序执行：

| 顺序 | 脚本 | 说明 |
|------|------|------|
| 1 | `init.sql` | 建库 + 基础表（user、checkin_record、goods 等） |
| 2 | `init_goods.sql` | 商品初始数据（如需要） |
| 3 | `migrate_growth_v1.sql` | 成长系统（level、exp、coin 等） |
| 4 | `migrate_m4_reminder.sql` | 提醒相关 |
| 5 | `migrate_m5.sql` | M5 相关 |
| 6 | `migrate_mysql57_compatible.sql` | **商城与成就**（5.7 兼容版） |

> **注意**：原 `migrate_store_achievement.sql` 使用了 `ADD COLUMN IF NOT EXISTS`，仅在 MySQL 8.0.12+ 支持。在 5.6/5.7 上请使用 `migrate_mysql57_compatible.sql`。

### 2. 应用配置

修改 `application.yml` 或使用环境变量，指向外部数据库：

```yaml
# 或通过环境变量
# MYSQL_ADDRESS=外部IP:3306
# MYSQL_USERNAME=root
# MYSQL_PASSWORD=你的密码

spring:
  datasource:
    url: jdbc:mysql://外部IP:3306/water_app?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

---

## 三、版本差异简要说明

| 特性 | MySQL 5.6 | MySQL 5.7 | MySQL 8.x |
|------|-----------|-----------|-----------|
| `ADD COLUMN IF NOT EXISTS` | ❌ | ❌ | ✅ 8.0.12+ |
| utf8mb4 | ✅ | ✅ | ✅ |
| InnoDB | ✅ | ✅ | ✅ |
| `ON UPDATE CURRENT_TIMESTAMP` | ✅ | ✅ | ✅ |
| JSON 类型 | ❌ | ✅ 5.7.8+ | ✅ |
| 默认认证插件 | mysql_native | mysql_native | caching_sha2_password |

本项目当前**未使用** JSON 列、GENERATED 列等，`init.sql` 与 `migrate_mysql57_compatible.sql` 在 5.6/5.7 上可正常运行。
