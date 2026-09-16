package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 兑换记录实体类
 */
@Data
@TableName("exchange_record")
public class ExchangeRecord {
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
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称（冗余）
     */
    private String goodsName;

    /**
     * 使用积分
     */
    private Integer pointsUsed;

    /**
     * 状态：0-待发货，1-已发货，2-已完成
     */
    private Integer status;

    /**
     * 收货地址
     */
    private String address;

    /**
     * 关联提醒ID（reward_reminder.id）
     */
    private Long reminderId;

    /**
     * 是否已执行：0-未执行，1-已执行
     */
    private Integer executed;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

