-- 清空商品图片URL（暂时不显示图片，避免加载错误）
-- 执行此SQL后，前端会显示"暂无图片"占位符

USE `water_app`;

-- 将所有商品的图片URL清空
UPDATE `goods` SET `image_url` = '' WHERE `image_url` IS NOT NULL AND `image_url` != '';

-- 验证更新结果
SELECT `id`, `name`, `image_url` FROM `goods` WHERE `status` = 1 ORDER BY `sort_order`;

-- 说明：
-- 1. 执行此SQL后，商品图片会显示"暂无图片"占位符
-- 2. 后续如果需要添加图片，可以：
--    - 将图片放到 backend/uploads/goods/ 目录
--    - 更新数据库：UPDATE `goods` SET `image_url` = '/uploads/goods/图片名.jpg' WHERE `name` = '商品名';
--    - 或者使用其他可访问的图片URL

