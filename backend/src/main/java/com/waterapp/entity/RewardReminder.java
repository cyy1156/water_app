package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 兑换后单次提醒
 */
@Data
@TableName("reward_reminder")
public class RewardReminder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long exchangeRecordId;
    private String content;
    private LocalDateTime remindTime;
    /** 状态：0-待发送，1-已发送 */
    private Integer status;
    private LocalDateTime createTime;
}
