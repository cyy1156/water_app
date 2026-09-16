-- 商城与成就改造：MySQL 5.6/5.7 兼容版迁移脚本
-- 与 migrate_store_achievement.sql 功能相同，但避免 ADD COLUMN IF NOT EXISTS（MySQL 8.0.12+ 才有）
-- 在 MySQL 5.6/5.7 上首次执行时使用本脚本；已执行过 migrate_store_achievement.sql 的库无需再执行
-- 执行前请备份数据库

USE `water_app`;

-- ========== 1.1 商城商品改造 ==========
-- MySQL 5.6/5.7 无 ADD COLUMN IF NOT EXISTS，用存储过程实现幂等
DELIMITER //
DROP PROCEDURE IF EXISTS _add_goods_type//
CREATE PROCEDURE _add_goods_type()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'goods' AND COLUMN_NAME = 'type') = 0 THEN
    ALTER TABLE `goods` ADD COLUMN `type` varchar(32) DEFAULT 'SELF_REWARD' COMMENT '商品类型：SELF_REWARD-自我奖励, PHYSICAL-实物' AFTER `description`;
  END IF;
END //
DELIMITER ;
CALL _add_goods_type();
DROP PROCEDURE IF EXISTS _add_goods_type;

UPDATE `goods` SET `type` = 'SELF_REWARD', `stock` = -1 WHERE `stock` IS NULL OR `stock` >= 0;

-- ========== 1.2 兑换记录改造 ==========
DELIMITER //
DROP PROCEDURE IF EXISTS _add_exchange_reminder_id//
CREATE PROCEDURE _add_exchange_reminder_id()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exchange_record' AND COLUMN_NAME = 'reminder_id') = 0 THEN
    ALTER TABLE `exchange_record` ADD COLUMN `reminder_id` bigint(20) DEFAULT NULL COMMENT '关联提醒ID（reward_reminder.id）' AFTER `address`;
  END IF;
END //
DELIMITER ;
CALL _add_exchange_reminder_id();
DROP PROCEDURE IF EXISTS _add_exchange_reminder_id;

DELIMITER //
DROP PROCEDURE IF EXISTS _add_exchange_executed//
CREATE PROCEDURE _add_exchange_executed()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exchange_record' AND COLUMN_NAME = 'executed') = 0 THEN
    ALTER TABLE `exchange_record` ADD COLUMN `executed` tinyint(4) DEFAULT 0 COMMENT '是否已执行：0-未执行，1-已执行' AFTER `reminder_id`;
  END IF;
END //
DELIMITER ;
CALL _add_exchange_executed();
DROP PROCEDURE IF EXISTS _add_exchange_executed;

UPDATE `exchange_record` SET `executed` = 0 WHERE `status` IN (0, 1);
UPDATE `exchange_record` SET `executed` = 1 WHERE `status` = 2;

-- ========== 1.3 成就表 ==========
CREATE TABLE IF NOT EXISTS `achievement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成就ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `type` varchar(32) NOT NULL COMMENT '类型：SYSTEM-系统目标, REDEEMED-兑换的奖励',
  `config_json` text COMMENT '配置JSON',
  `completed_at` datetime DEFAULT NULL COMMENT '达成时间',
  `exchange_record_id` bigint(20) DEFAULT NULL COMMENT '兑换记录ID（type=REDEEMED时）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `type`),
  KEY `idx_user_time` (`user_id`, `completed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就表';

-- ========== 1.4 系统成就配置表 ==========
CREATE TABLE IF NOT EXISTS `achievement_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `streak_days` int(11) NOT NULL COMMENT '连续打卡天数',
  `badge_name` varchar(64) DEFAULT NULL COMMENT '徽章名称',
  `coin_reward` int(11) DEFAULT 0 COMMENT '金币奖励',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_streak` (`streak_days`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统成就配置表';

INSERT INTO `achievement_config` (`streak_days`, `badge_name`, `coin_reward`, `sort_order`) VALUES
(3, '三日坚持', 30, 1),
(5, '五日坚持', 50, 2),
(7, '一周坚持', 80, 3),
(10, '十日坚持', 120, 4),
(14, '两周坚持', 180, 5),
(21, '三周坚持', 250, 6),
(30, '一月坚持', 400, 7)
ON DUPLICATE KEY UPDATE `badge_name`=VALUES(`badge_name`), `coin_reward`=VALUES(`coin_reward`), `sort_order`=VALUES(`sort_order`);

-- ========== 1.5 兑换后单次提醒表 ==========
CREATE TABLE IF NOT EXISTS `reward_reminder` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '提醒ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `exchange_record_id` bigint(20) NOT NULL COMMENT '兑换记录ID',
  `content` varchar(255) DEFAULT NULL COMMENT '提醒内容',
  `remind_time` datetime DEFAULT NULL COMMENT '提醒时间',
  `status` tinyint(4) DEFAULT 0 COMMENT '状态：0-待发送，1-已发送',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_exchange` (`exchange_record_id`),
  KEY `idx_remind_time` (`remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换后单次提醒表';
