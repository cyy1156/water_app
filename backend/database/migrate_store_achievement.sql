-- 商城与成就改造：数据模型迁移（幂等，可重复执行）
-- 执行前请备份数据库
-- 要求：MySQL 8.0.12+（ADD COLUMN IF NOT EXISTS）

USE `water_app`;

-- ========== 1.1 商城商品改造 ==========
ALTER TABLE `goods`
  ADD COLUMN IF NOT EXISTS `type` varchar(32) DEFAULT 'SELF_REWARD' COMMENT '商品类型：SELF_REWARD-自我奖励, PHYSICAL-实物' AFTER `description`;

UPDATE `goods` SET `type` = 'SELF_REWARD', `stock` = -1 WHERE `stock` IS NULL OR `stock` >= 0;

-- ========== 1.2 兑换记录改造 ==========
ALTER TABLE `exchange_record`
  ADD COLUMN IF NOT EXISTS `reminder_id` bigint(20) DEFAULT NULL COMMENT '关联提醒ID（reward_reminder.id）' AFTER `address`,
  ADD COLUMN IF NOT EXISTS `executed` tinyint(4) DEFAULT 0 COMMENT '是否已执行：0-未执行，1-已执行' AFTER `reminder_id`;

-- 旧数据状态映射：待发货/已发货/已完成 -> 待执行/已执行
UPDATE `exchange_record` SET `executed` = 0 WHERE `status` IN (0, 1);
UPDATE `exchange_record` SET `executed` = 1 WHERE `status` = 2;

-- ========== 1.3 成就表 ==========
CREATE TABLE IF NOT EXISTS `achievement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成就ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `type` varchar(32) NOT NULL COMMENT '类型：SYSTEM-系统目标, REDEEMED-兑换的奖励',
  `config_json` text COMMENT '配置JSON：系统成就存连续天数/徽章名/金币；兑换成就存商品名等',
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

-- 插入默认档位：3/5/7/10/14/21/30 天
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
  `content` varchar(255) DEFAULT NULL COMMENT '提醒内容（如：奖励自己喝一杯奶茶）',
  `remind_time` datetime DEFAULT NULL COMMENT '提醒时间',
  `status` tinyint(4) DEFAULT 0 COMMENT '状态：0-待发送，1-已发送',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_exchange` (`exchange_record_id`),
  KEY `idx_remind_time` (`remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换后单次提醒表';

-- ========== 1.6 自我奖励商品初始化（可选，已注释） ==========
-- 如需把现有商品改成自我奖励，请取消下方注释

UPDATE `goods` SET
  `type` = 'SELF_REWARD',
  `stock` = -1,
  `name` = CASE `id`
    WHEN 1 THEN '奖励自己喝一杯奶茶'
    WHEN 2 THEN '奖励自己买一件衣服'
    WHEN 3 THEN '奖励自己看一场电影'
    WHEN 4 THEN '奖励自己清空购物车第一件'
    WHEN 5 THEN '奖励自己吃一顿大餐'
    WHEN 6 THEN '奖励自己周末睡个懒觉'
    ELSE `name`
  END,
  `description` = CASE `id`
    WHEN 1 THEN '达成目标后奖励自己喝一杯喜欢的奶茶'
    WHEN 2 THEN '达成目标后奖励自己买一件心仪的衣服'
    WHEN 3 THEN '达成目标后奖励自己看一场想看的电影'
    WHEN 4 THEN '达成目标后奖励自己清空购物车第一件商品'
    WHEN 5 THEN '达成目标后奖励自己吃一顿丰盛的大餐'
    WHEN 6 THEN '达成目标后奖励自己周末睡个懒觉'
    ELSE `description`
  END;

