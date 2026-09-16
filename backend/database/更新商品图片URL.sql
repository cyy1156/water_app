-- 更新商品图片URL（使用国内可访问的占位图片）
-- 执行此SQL可以快速修复图片加载问题

USE `water_app`;

-- 方案1：使用 placeholder.com 占位图片（推荐，国内可访问）
UPDATE `goods` SET `image_url` = 'https://via.placeholder.com/300x300/4A90E2/FFFFFF?text=精美水杯' WHERE `name` = '精美水杯';
UPDATE `goods` SET `image_url` = 'https://via.placeholder.com/300x300/50C878/FFFFFF?text=健康手册' WHERE `name` = '健康手册';
UPDATE `goods` SET `image_url` = 'https://via.placeholder.com/300x300/FF6B6B/FFFFFF?text=定制T恤' WHERE `name` = '定制T恤';
UPDATE `goods` SET `image_url` = 'https://via.placeholder.com/300x300/4ECDC4/FFFFFF?text=运动手环' WHERE `name` = '运动手环';
UPDATE `goods` SET `image_url` = 'https://via.placeholder.com/300x300/FFE66D/FFFFFF?text=水杯套装' WHERE `name` = '水杯套装';
UPDATE `goods` SET `image_url` = 'https://via.placeholder.com/300x300/95E1D3/FFFFFF?text=健康秤' WHERE `name` = '健康秤';

-- 验证更新结果
SELECT `id`, `name`, `image_url` FROM `goods` WHERE `status` = 1 ORDER BY `sort_order`;

-- 说明：
-- 1. placeholder.com 是免费的占位图片服务，国内可以访问
-- 2. 格式：https://via.placeholder.com/宽度x高度/背景色/文字颜色?text=文字
-- 3. 如果需要使用真实图片，请：
--    - 上传图片到 sm.ms (https://sm.ms)
--    - 复制图片URL
--    - 执行 UPDATE 语句更新 image_url

