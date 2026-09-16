-- 大升级 M1：成长系统最小迁移（level/exp/coin/stage + checkin扩展）
-- 说明：
-- 1) 保持兼容：不删除旧字段 total_points / points_earned
-- 2) coin 字段启用后，建议业务逐步以 coin 为准，total_points 可作为历史兼容字段

USE `water_app`;

-- user 表扩展：等级/经验/金币/成长阶段
ALTER TABLE `user`
  ADD COLUMN `level` int(11) NOT NULL DEFAULT 1 COMMENT '等级' AFTER `total_points`,
  ADD COLUMN `exp` int(11) NOT NULL DEFAULT 0 COMMENT '当前经验值' AFTER `level`,
  ADD COLUMN `coin` int(11) NOT NULL DEFAULT 0 COMMENT '金币余额' AFTER `exp`,
  ADD COLUMN `growth_stage` varchar(20) DEFAULT '幼苗' COMMENT '成长阶段' AFTER `coin`;

-- checkin_record 表扩展：经验/金币结算字段
ALTER TABLE `checkin_record`
  ADD COLUMN `exp_earned` int(11) NOT NULL DEFAULT 0 COMMENT '获得经验' AFTER `points_earned`,
  ADD COLUMN `coin_earned` int(11) NOT NULL DEFAULT 0 COMMENT '获得金币' AFTER `exp_earned`,
  ADD COLUMN `is_target_bonus` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否触发目标达成奖励' AFTER `coin_earned`;


