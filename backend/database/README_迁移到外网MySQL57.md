# 本地数据库迁移到外网 MySQL 5.7

将本地的 `water_app` 数据库迁移到腾讯云外网 MySQL 5.7。

## 外网数据库连接信息

| 项目 | 值 |
|------|-----|
| 外网地址 | `<YOUR_MYSQL_HOST>:3306` |
| 用户名 | root |
| 密码 | <YOUR_MYSQL_PASSWORD> |

（已写入 `migrate_to_external_mysql57.bat`，可在脚本内或通过环境变量修改）

## 使用步骤

### 1. 设置本地 MySQL 密码

在运行脚本前，任选其一：

**方式 A：环境变量**
```bat
set LOCAL_MYSQL_PASS=你的本地MySQL密码
migrate_to_external_mysql57.bat
```

**方式 B：直接运行**  
脚本会提示输入本地 root 密码。

### 2. 执行迁移

```bat
cd D:\小程序\backend\database
migrate_to_external_mysql57.bat
```

### 3. 脚本执行流程

1. 从本地 MySQL 导出 `water_app`（含结构和数据）
2. 导入到外网 MySQL 5.7
3. 删除临时导出文件

## 若外网导入失败

- **Unknown collation 'utf8mb4_0900_ai_ci'**：本地 MySQL 8.0 的排序规则在 5.7 中不存在。脚本已自动将 `utf8mb4_0900_*` 替换为 `utf8mb4_unicode_ci`，一般可解决。
- **SSL 错误**：脚本中已加 `--ssl-mode=DISABLED`，如仍报错可尝试删除该参数
- **连接超时**：检查本机 IP 是否已加入腾讯云数据库外网白名单
- **权限错误**：确认 root 有创建库、建表、导入等权限

## 迁移后配置应用

微信云托管等环境变量示例：

```
MYSQL_ADDRESS=<YOUR_MYSQL_HOST>:3306
MYSQL_USERNAME=root
MYSQL_PASSWORD=<YOUR_MYSQL_PASSWORD>
```

> `application.yml` 中通过 `MYSQL_ADDRESS`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 读取，云托管配置后会自动生效。
