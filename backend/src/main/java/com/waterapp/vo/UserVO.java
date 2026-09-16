package com.waterapp.vo;

import lombok.Data;

/**
 * 用户视图对象
 */
@Data
public class UserVO {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 每日目标水量(ml)
     */
    private Integer targetWater;

    /**
     * 总积分
     */
    private Integer totalPoints;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 封面文字
     */
    private String coverText;
}

