package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录实体类
 */
@Data
@TableName("checkin_record")
public class CheckinRecord {
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
     * 喝水量(ml)
     */
    private Integer waterMl;

    /**
     * 喝水品种（M5）：WATER/TEA/COFFEE/MILK_TEA/DRINK
     */
    private String waterType;

    /**
     * 打卡时间
     */
    private LocalDateTime checkinTime;

    /**
     * 打卡日期
     */
    private LocalDate checkinDate;

    /**
     * 获得积分
     */
    private Integer pointsEarned;

    /**
     * 获得经验（大升级：成长系统）
     */
    private Integer expEarned;

    /**
     * 获得金币（大升级：成长系统）
     */
    private Integer coinEarned;

    /**
     * 是否触发目标达成奖励
     */
    private Integer isTargetBonus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

