-- 大升级 M4：粥粥提醒系统 + 话术模板（预留）
-- 说明：为订阅消息、应用内提醒、话术库提供数据支撑

USE `water_app`;

-- 1) 话术模板库（运营可配置）
CREATE TABLE IF NOT EXISTS `ai_phrase_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene` varchar(30) NOT NULL COMMENT '场景枚举：GREETING/REMIND_SOFT/AFTER_CHECKIN/TARGET_REACHED等',
  `template` varchar(500) NOT NULL COMMENT '模板文本，支持变量：{nickname}{waterMl}{currentWater}{targetWater}{exp}{coin}{level}',
  `weight` int NOT NULL DEFAULT 1 COMMENT '权重（随机挑选时使用）',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene_enabled` (`scene`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI话术模板库';

-- 2) 用户提醒设置
CREATE TABLE IF NOT EXISTS `user_reminder_setting` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否开启提醒：0否 1是',
  `start_time` time DEFAULT '09:00:00' COMMENT '提醒开始时段',
  `end_time` time DEFAULT '22:00:00' COMMENT '提醒结束时段',
  `interval_min` int NOT NULL DEFAULT 90 COMMENT '提醒间隔（分钟）',
  `intensity` varchar(20) DEFAULT 'SOFT' COMMENT '提醒强度：SOFT/MEDIUM/STRONG',
  `quiet_after_target` tinyint NOT NULL DEFAULT 1 COMMENT '达标后当日不再提醒：0否 1是',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户提醒设置';

-- 3) 提醒日志（防重复 + 可观测）
CREATE TABLE IF NOT EXISTS `reminder_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `scene` varchar(30) DEFAULT NULL COMMENT '场景',
  `content` varchar(500) DEFAULT NULL COMMENT '提醒内容',
  `channel` varchar(20) DEFAULT NULL COMMENT '渠道：IN_APP/SUBSCRIBE_MSG',
  `status` varchar(20) DEFAULT NULL COMMENT '状态：SENT/FAILED/SKIPPED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒发送日志';

-- 插入默认话术模板（A档可爱软萌）
INSERT IGNORE INTO `ai_phrase_template` (`scene`, `template`, `weight`, `enabled`) VALUES
('GREETING', '嗨 {nickname}～我是粥粥🌱 今天也一起把水喝得亮晶晶吧！', 1, 1),
('REMIND_SOFT', '粥粥轻轻敲敲杯子～咕嘟一口水好吗？💧', 1, 1),
('AFTER_CHECKIN', '叮咚！你喝了 {waterMl}ml～粥粥给你贴贴夸夸！', 1, 1),
('TARGET_NEAR', '哇～只差一点点就达标啦！再来一小口就赢！', 1, 1),
('TARGET_REACHED', '达标啦！粥粥给你撒花～今天的你超厉害！🎉', 1, 1),
('LEVEL_UP', '升级成功！Lv.{level} 的你更闪闪发光了～', 1, 1),
('COIN_NOT_ENOUGH', '唔…金币还差一点点～我们多喝几口水滴滴就有啦！', 1, 1);

