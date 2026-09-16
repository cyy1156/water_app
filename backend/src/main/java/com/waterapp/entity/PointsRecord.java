package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分记录实体类
 */
@Data
@TableName("points_record")
public class PointsRecord {
    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 积分变动（正数增加，负数减少）
     */
    private Integer points;

    /**
     * 类型：CHECKIN-打卡, EXCHANGE-兑换, OTHER-其他
     */
    private String type;

    /**
     * 关联ID（如打卡记录ID、兑换记录ID）
     */
    private Long relatedId;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

