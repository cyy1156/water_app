package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统成就配置
 */
@Data
@TableName("achievement_config")
public class AchievementConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer streakDays;
    private String badgeName;
    private Integer coinReward;
    private Integer sortOrder;
}
