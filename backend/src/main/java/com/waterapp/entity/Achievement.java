package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就实体
 */
@Data
@TableName("achievement")
public class Achievement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 类型：SYSTEM-系统目标，REDEEMED-兑换的奖励 */
    private String type;
    /** 配置JSON：系统成就存连续天数/徽章名/金币；兑换成就存商品名等 */
    private String configJson;
    private LocalDateTime completedAt;
    private Long exchangeRecordId;
    private LocalDateTime createTime;
}
