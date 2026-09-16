package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户提醒设置（M4：粥粥喝水提醒）
 */
@Data
@TableName("user_reminder_setting")
public class UserReminderSetting {

    @TableId(type = IdType.INPUT)
    private Long userId;

    /** 是否开启提醒：0否 1是 */
    private Integer enabled;

    /** 提醒开始时段 */
    private LocalTime startTime;

    /** 提醒结束时段 */
    private LocalTime endTime;

    /** 提醒间隔（分钟） */
    private Integer intervalMin;

    /** 提醒强度：SOFT/MEDIUM/STRONG */
    private String intensity;

    /** 达标后当日不再提醒：0否 1是 */
    private Integer quietAfterTarget;

    private LocalDateTime updateTime;
}

