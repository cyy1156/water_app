USE `water_app`;

-- 将 image_url 更新为本地静态资源（这些文件目前存在于：D:\小程序\backend\uploads\goods\）
-- 注意：后端配置了 context-path: /api，因此最终访问形如：
-- http://192.168.200.120:8080/api/uploads/goods/set.png

UPDATE `goods` SET `image_url` = '/uploads/goods/cup.png'
WHERE `name` = '精美水杯';

UPDATE `goods` SET `image_url` = '/uploads/goods/book.png'
WHERE `name` = '健康手册';

UPDATE `goods` SET `image_url` = '/uploads/goods/Tshirt.png'
WHERE `name` = '定制T恤';

UPDATE `goods` SET `image_url` = '/uploads/goods/watch.png'
WHERE `name` = '运动手环';

UPDATE `goods` SET `image_url` = '/uploads/goods/set.png'
WHERE `name` = '水杯套装';

UPDATE `goods` SET `image_url` = '/uploads/goods/scale.png'
WHERE `name` = '健康秤';

-- 验证
SELECT `id`, `name`, `image_url` FROM `goods` WHERE `status` = 1 ORDER BY `sort_order`;


