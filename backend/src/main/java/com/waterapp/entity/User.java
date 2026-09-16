package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User {
    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 微信openid
     */
    private String openid;

    /**
     * 微信unionid
     */
    private String unionid;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 性别（M5）：0未知/1男/2女
     */
    private Integer gender;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 每日目标水量(ml)
     */
    private Integer targetWater;

    /**
     * 个性化推荐目标（M5，首次登录时设置）
     */
    private Integer personalizedTarget;

    /**
     * 总积分
     */
    private Integer totalPoints;

    /**
     * 等级（大升级：成长系统）
     */
    private Integer level;

    /**
     * 当前等级经验值（大升级：成长系统）
     */
    private Integer exp;

    /**
     * 金币余额（大升级：成长系统）
     */
    private Integer coin;

    /**
     * 成长阶段（幼苗/成长中/活力粥粥）
     */
    private String growthStage;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 封面文字
     */
    private String coverText;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 首次登录时间（M5）
     */
    private LocalDateTime firstLoginTime;
}

