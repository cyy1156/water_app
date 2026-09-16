package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒日志实体（M4：防重复 + 可观测）
 */
@Data
@TableName("reminder_log")
public class ReminderLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 场景枚举（如 REMIND_SOFT） */
    private String scene;

    /** 提醒内容 */
    private String content;

    /** 发送渠道：IN_APP/SUBSCRIBE_MSG */
    private String channel;

    /** 发送状态：SENT/FAILED/SKIPPED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}

