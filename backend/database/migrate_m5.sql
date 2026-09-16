-- M5 升级：喝水品种、用户个性化信息
-- 执行前请备份数据库

USE `water_app`;

-- 1. checkin_record 表增加 water_type 字段
ALTER TABLE `checkin_record`
  ADD COLUMN `water_type` varchar(20) DEFAULT 'WATER' COMMENT '喝水品种：WATER(白水)/TEA(茶)/COFFEE(咖啡)/MILK_TEA(奶茶)/DRINK(饮料)' AFTER `water_ml`;

-- 2. user 表增加个性化相关字段
ALTER TABLE `user`
  ADD COLUMN `gender` tinyint(4) DEFAULT 0 COMMENT '性别：0未知/1男/2女' AFTER `nickname`,
  ADD COLUMN `first_login_time` datetime DEFAULT NULL COMMENT '首次登录时间' AFTER `update_time`,
  ADD COLUMN `personalized_target` int(11) DEFAULT NULL COMMENT '个性化推荐目标（首次登录时设置）' AFTER `target_water`;

