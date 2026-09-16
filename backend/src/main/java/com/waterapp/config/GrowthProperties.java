package com.waterapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 成长系统配置（可在 application.yml 中配置）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "growth")
public class GrowthProperties {

    /** 每 100ml 获得的 EXP */
    private int expPer100ml = 10;

    /** 每 100ml 获得的金币 */
    private int coinPer100ml = 5;

    /** 达到目标水量时一次性奖励 EXP */
    private int targetBonusExp = 100;

    /** 达到目标水量时一次性奖励金币 */
    private int targetBonusCoin = 50;

    /**
     * 升级所需经验（单级阈值，当未配置 levelExpTable 时使用）
     */
    private int nextLevelExp = 1000;

    /**
     * 多级经验阈值：下标 0 表示 Lv1→2 所需经验，下标 1 表示 Lv2→3，以此类推。
     * 未配置或列表为空时使用 nextLevelExp。
     */
    private List<Integer> levelExpTable = new ArrayList<>();
}


