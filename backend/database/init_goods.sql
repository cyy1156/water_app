-- 商品数据初始化脚本
-- 执行前请先确认 goods 表已创建

USE `water_app`;

-- 清空现有商品数据（可选，谨慎使用）
 TRUNCATE TABLE `goods`;

-- 插入示例商品数据
-- 注意：图片URL使用 placeholder.com 占位图片（国内可访问）
-- 如需使用真实图片，请上传到 sm.ms 或其他国内图床，然后更新 image_url
INSERT INTO `goods` (`name`, `description`, `image_url`, `points_required`, `stock`, `status`, `sort_order`, `create_time`, `update_time`) VALUES
('精美水杯', '高品质不锈钢保温杯，容量500ml，保温12小时', 'https://via.placeholder.com/300x300/4A90E2/FFFFFF?text=精美水杯', 500, 100, 1, 1, NOW(), NOW()),
('健康手册', '健康饮水指南电子书，包含科学饮水方法和健康小贴士', 'https://via.placeholder.com/300x300/50C878/FFFFFF?text=健康手册', 800, 999, 1, 2, NOW(), NOW()),
('定制T恤', '印有"多喝水"字样的定制T恤，100%纯棉材质', 'https://via.placeholder.com/300x300/FF6B6B/FFFFFF?text=定制T恤', 1200, 50, 1, 3, NOW(), NOW()),
('运动手环', '智能运动手环，记录运动数据、睡眠质量，支持心率监测', 'https://via.placeholder.com/300x300/4ECDC4/FFFFFF?text=运动手环', 2000, 30, 1, 4, NOW(), NOW()),
('水杯套装', '包含大中小三个尺寸的水杯套装，满足不同场景需求', 'https://via.placeholder.com/300x300/FFE66D/FFFFFF?text=水杯套装', 1500, 80, 1, 5, NOW(), NOW()),
('健康秤', '智能体脂秤，精准测量体重、体脂率、肌肉量等多项指标', 'https://via.placeholder.com/300x300/95E1D3/FFFFFF?text=健康秤', 3000, 20, 1, 6, NOW(), NOW());

-- 查询验证
SELECT * FROM `goods` WHERE `status` = 1 ORDER BY `sort_order`;

-- 说明：
-- 1. image_url 字段暂时为空，后续可以通过管理接口或直接更新数据库添加图片URL
-- 2. 可以根据实际需求修改商品信息
-- 3. status: 0-下架，1-上架
-- 4. sort_order: 排序，数字越小越靠前

