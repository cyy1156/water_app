-- ============================================================
-- MySQL 5.6 / 5.7 兼容性测试脚本
-- 在目标数据库（外部 MySQL 5.6/5.7）上执行，用于验证兼容性
-- ============================================================

-- 1. 查看 MySQL 版本（应显示 5.6.x 或 5.7.x）
SELECT VERSION() AS mysql_version;

-- 2. 检查字符集支持（water_app 使用 utf8mb4）
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';

-- 3. 检查 SQL 模式（影响严格模式、零日期等）
SELECT @@sql_mode AS sql_mode;

-- 4. 检查 InnoDB 引擎是否可用
SHOW ENGINES;

-- 5. 若 water_app 已存在，检查数据库
SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'water_app';

-- 6. 简单建表测试（MySQL 5.6/5.7 兼容语法）
-- 使用 CREATE TABLE IF NOT EXISTS、DEFAULT CURRENT_TIMESTAMP 等
-- 执行后删除测试表
CREATE DATABASE IF NOT EXISTS `water_app` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `water_app`;

-- 测试表：使用与项目相同的常见语法
CREATE TABLE IF NOT EXISTS `_compat_test` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `_compat_test` (`name`) VALUES ('test');
SELECT * FROM `_compat_test`;

-- 清理测试表
DROP TABLE IF EXISTS `_compat_test`;

-- 7. 输出结果摘要
SELECT 'MySQL 5.6/5.7 兼容性检查完成' AS result, VERSION() AS version;
