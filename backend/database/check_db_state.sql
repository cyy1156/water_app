-- 数据库状态检查（商城与成就迁移相关）
-- 执行方式：mysql -u root -p water_app < check_db_state.sql
-- 或在 MySQL 客户端中执行

USE `water_app`;

SELECT '========== 1. goods 表结构（是否有 type 列）==========' AS '';
SHOW COLUMNS FROM `goods` LIKE 'type';

SELECT '========== 2. goods 当前数据（name, description, type, stock）==========' AS '';
SELECT id, name, description, `type`, stock, points_required FROM `goods` LIMIT 10;

SELECT '========== 3. exchange_record 表结构（是否有 reminder_id, executed）==========' AS '';
SHOW COLUMNS FROM `exchange_record` LIKE 'reminder_id';
SHOW COLUMNS FROM `exchange_record` LIKE 'executed';

SELECT '========== 4. 是否存在 achievement 表 ==========' AS '';
SELECT COUNT(*) AS achievement_table_exists FROM information_schema.tables 
WHERE table_schema = 'water_app' AND table_name = 'achievement';

SELECT '========== 5. 是否存在 achievement_config 表 ==========' AS '';
SELECT COUNT(*) AS achievement_config_exists FROM information_schema.tables 
WHERE table_schema = 'water_app' AND table_name = 'achievement_config';

SELECT '========== 6. 是否存在 reward_reminder 表 ==========' AS '';
SELECT COUNT(*) AS reward_reminder_exists FROM information_schema.tables 
WHERE table_schema = 'water_app' AND table_name = 'reward_reminder';
