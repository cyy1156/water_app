-- 检查商品重复的SQL脚本
-- 使用方法：在MySQL客户端中执行此脚本

USE `water_app`;

-- 1. 查看所有商品（包括重复的）
SELECT 
    `id`, 
    `name`, 
    `description`, 
    `image_url`, 
    `points_required`, 
    `stock`, 
    `status`, 
    `sort_order`,
    `create_time`
FROM `goods`
ORDER BY `name`, `id`;

-- 2. 检查是否有重复的商品名称
SELECT 
    `name`, 
    COUNT(*) as `重复数量`
FROM `goods`
GROUP BY `name`
HAVING COUNT(*) > 1;

-- 3. 查看重复商品的详细信息
SELECT 
    g1.`id`,
    g1.`name`,
    g1.`points_required`,
    g1.`stock`,
    g1.`status`,
    g1.`create_time`
FROM `goods` g1
INNER JOIN (
    SELECT `name`
    FROM `goods`
    GROUP BY `name`
    HAVING COUNT(*) > 1
) g2 ON g1.`name` = g2.`name`
ORDER BY g1.`name`, g1.`id`;

-- 4. 统计商品总数
SELECT COUNT(*) as `商品总数` FROM `goods`;
SELECT COUNT(*) as `上架商品数` FROM `goods` WHERE `status` = 1;

-- 5. 删除重复商品（保留ID最小的，谨慎使用！）
-- 如果需要删除重复商品，可以先执行以下查询查看会删除哪些记录：
/*
SELECT 
    g1.`id`,
    g1.`name`,
    g1.`create_time`
FROM `goods` g1
INNER JOIN (
    SELECT `name`, MIN(`id`) as `min_id`
    FROM `goods`
    GROUP BY `name`
    HAVING COUNT(*) > 1
) g2 ON g1.`name` = g2.`name` AND g1.`id` > g2.`min_id`;
*/

-- 实际删除重复商品（取消注释后执行，会删除除最小ID外的重复商品）
-- DELETE g1 FROM `goods` g1
-- INNER JOIN (
--     SELECT `name`, MIN(`id`) as `min_id`
--     FROM `goods`
--     GROUP BY `name`
--     HAVING COUNT(*) > 1
-- ) g2 ON g1.`name` = g2.`name` AND g1.`id` > g2.`min_id`;

