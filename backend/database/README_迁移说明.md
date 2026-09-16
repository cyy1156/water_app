# 数据库迁移说明

## 快速执行

双击运行 **`run_migrate_all.bat`**，将按顺序执行所有迁移脚本。

## 迁移顺序

| 顺序 | 文件 | 说明 |
|------|------|------|
| 1 | migrate_growth_v1.sql | M1 成长系统（level/exp/coin/stage） |
| 2 | migrate_m4_reminder.sql | M4 粥粥提醒（user_reminder_setting 等） |
| 3 | migrate_m5.sql | M5 喝水品种、用户个性化（water_type/gender 等） |

## 环境变量（可选）

与 `application.yml` 保持一致，可在执行前设置：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| MYSQL_ADDRESS | localhost:3306 | MySQL 地址 |
| MYSQL_USERNAME | root | 用户名 |
| MYSQL_PASSWORD | （无） | 密码，设置后无需手动输入 |
| MYSQL_DATABASE | water_app | 数据库名 |

## 示例

```bat
:: 使用环境变量（不提示密码）
set MYSQL_PASSWORD=你的密码
run_migrate_all.bat

:: 指定远程数据库
set MYSQL_ADDRESS=192.168.1.100:3306
set MYSQL_USERNAME=root
set MYSQL_PASSWORD=xxx
run_migrate_all.bat
```

## 单独执行某个迁移

如需只执行某一阶段：

- `run_migrate_growth_v1.bat` - 仅 M1
- `run_migrate_m4.bat` - 仅 M4
- `run_migrate_m5.bat` - 仅 M5

## 注意事项

1. **执行前请备份数据库**
2. 若某次迁移已执行过，再次运行可能报错（如「列已存在」），可忽略该迁移或手动跳过对应 SQL
3. 确保 MySQL 客户端在 PATH 中，或取消 `run_migrate_growth_v1.bat` 中的 PATH 注释并修改为你的 MySQL 安装路径

