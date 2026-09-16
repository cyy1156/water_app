package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waterapp.config.GrowthProperties;
import com.waterapp.common.exception.BusinessException;
import com.waterapp.entity.CheckinRecord;
import com.waterapp.entity.PointsRecord;
import com.waterapp.entity.User;
import com.waterapp.mapper.CheckinRecordMapper;
import com.waterapp.mapper.PointsRecordMapper;
import com.waterapp.mapper.UserMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成长系统结算服务
 * - 以喝水打卡为事实来源
 * - 统一结算 EXP / Coin / 升级 / 达标奖励
 *
 * 说明：为了兼容旧系统，Coin 结算会同步写入 user.totalPoints（旧积分）字段。
 */
@Slf4j
@Service
public class GrowthService {

    @Autowired
    private GrowthProperties growthProperties;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CheckinRecordMapper checkinRecordMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    public String stageFromLevel(int level) {
        if (level <= 3) return "幼苗";
        if (level <= 7) return "成长中";
        return "活力粥粥";
    }

    public int calcExpEarned(int waterMl) {
        return (waterMl / 100) * growthProperties.getExpPer100ml();
    }

    public int calcCoinEarned(int waterMl) {
        return (waterMl / 100) * growthProperties.getCoinPer100ml();
    }

    /**
     * 当前等级升到下一级所需经验。
     * 若配置了 level-exp-table，按等级取对应下标；否则使用 next-level-exp。
     */
    public int nextLevelExp(int level) {
        List<Integer> table = growthProperties.getLevelExpTable();
        if (table != null && !table.isEmpty()) {
            int index = level - 1; // Lv1 对应下标 0
            if (index >= 0 && index < table.size()) {
                return table.get(index);
            }
            return table.get(table.size() - 1);
        }
        return growthProperties.getNextLevelExp();
    }

    public TodayWaterSummary getTodayWaterSummary(Long userId) {
        LocalDate today = LocalDate.now();
        List<CheckinRecord> records = checkinRecordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .eq(CheckinRecord::getCheckinDate, today)
        );
        int sum = 0;
        LocalDateTime lastTime = null;
        for (CheckinRecord r : records) {
            sum += (r.getWaterMl() == null ? 0 : r.getWaterMl());
            if (lastTime == null || (r.getCheckinTime() != null && r.getCheckinTime().isAfter(lastTime))) {
                lastTime = r.getCheckinTime();
            }
        }
        TodayWaterSummary s = new TodayWaterSummary();
        s.setCurrentWater(sum);
        s.setLastCheckinTime(lastTime);
        return s;
    }

    /**
     * 喝水打卡 + 成长结算
     * @param waterType M5：喝水品种 WATER/TEA/COFFEE/MILK_TEA/DRINK，默认 WATER
     */
    @Transactional
    public CheckinSettlementResult checkinAndSettle(Long userId, int waterMl, String waterType) {
        if (userId == null) throw new BusinessException("未登录");
        if (waterMl <= 0) throw new BusinessException("喝水量必须大于0");

        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        // 确保成长字段默认值存在（历史用户兼容）
        Integer levelObj = user.getLevel();
        Integer expObj = user.getExp();
        Integer coinObj = user.getCoin();
        int level = levelObj == null ? 1 : levelObj;
        int exp = expObj == null ? 0 : expObj;
        int coin = coinObj == null ? (user.getTotalPoints() == null ? 0 : user.getTotalPoints()) : coinObj;

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 计算打卡前今日饮水
        int beforeWater = 0;
        {
            Integer sum = checkinRecordMapper.selectList(
                    new LambdaQueryWrapper<CheckinRecord>()
                            .eq(CheckinRecord::getUserId, userId)
                            .eq(CheckinRecord::getCheckinDate, today)
            ).stream().mapToInt(r -> r.getWaterMl() == null ? 0 : r.getWaterMl()).sum();
            beforeWater = sum;
        }

        int targetWater = user.getTargetWater() == null ? 2000 : user.getTargetWater();
        boolean beforeReached = beforeWater >= targetWater;

        // 新增打卡记录（先插入拿到 id）
        String wt = (waterType != null && !waterType.trim().isEmpty()) ? waterType.trim().toUpperCase() : "WATER";
        if (!"WATER".equals(wt) && !"TEA".equals(wt) && !"COFFEE".equals(wt) && !"MILK_TEA".equals(wt) && !"DRINK".equals(wt)) {
            wt = "WATER";
        }
        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setWaterMl(waterMl);
        record.setWaterType(wt);
        record.setCheckinTime(now);
        record.setCheckinDate(today);
        record.setCreateTime(now);
        checkinRecordMapper.insert(record);

        // 本次结算
        int expEarned = calcExpEarned(waterMl);
        int coinEarned = calcCoinEarned(waterMl);

        exp += expEarned;
        coin += coinEarned;

        // 目标达成奖励（一次性）
        int bonusExp = 0;
        int bonusCoin = 0;
        boolean bonusTriggered = false;

        int afterWater = beforeWater + waterMl;
        boolean afterReached = afterWater >= targetWater;
        if (!beforeReached && afterReached) {
            bonusTriggered = true;
            bonusExp = growthProperties.getTargetBonusExp();
            bonusCoin = growthProperties.getTargetBonusCoin();
            exp += bonusExp;
            coin += bonusCoin;
        }

        // 升级（循环处理多次升级）
        boolean leveledUp = false;
        int threshold = nextLevelExp(level);
        while (threshold > 0 && exp >= threshold) {
            exp -= threshold;
            level += 1;
            leveledUp = true;
            threshold = nextLevelExp(level);
        }

        String stage = stageFromLevel(level);

        // 回写 user
        user.setLevel(level);
        user.setExp(exp);
        user.setCoin(coin);
        user.setGrowthStage(stage);
        // 兼容：同步旧积分字段
        user.setTotalPoints(coin);
        user.setUpdateTime(now);
        userMapper.updateById(user);

        // 回写 checkin_record 扩展字段
        record.setExpEarned(expEarned);
        record.setCoinEarned(coinEarned);
        record.setIsTargetBonus(bonusTriggered ? 1 : 0);
        checkinRecordMapper.updateById(record);

        // 写金币流水（复用 points_record）
        insertCoinLedger(userId, coinEarned, "CHECKIN", record.getId(), "喝水打卡获得金币");
        if (bonusTriggered) {
            insertCoinLedger(userId, bonusCoin, "BONUS", record.getId(), "达成目标奖励金币");
        }

        // 连续打卡成就检查与发放
        try {
            achievementService.checkAndGrantStreakAchievements(userId);
        } catch (Exception e) {
            log.warn("连续打卡成就检查失败", e);
        }

        CheckinSettlementResult result = new CheckinSettlementResult();
        result.setRecord(record);
        result.setLevel(level);
        result.setExp(exp);
        result.setNextLevelExp(nextLevelExp(level));
        result.setCoin(coin);
        result.setStage(stage);
        result.setLeveledUp(leveledUp);
        result.setCurrentWater(afterWater);
        result.setTargetWater(targetWater);

        Delta delta = new Delta();
        delta.setWaterMl(waterMl);
        delta.setExp(expEarned + bonusExp);
        delta.setCoin(coinEarned + bonusCoin);
        TargetBonus tb = new TargetBonus();
        tb.setTriggered(bonusTriggered);
        tb.setExp(bonusExp);
        tb.setCoin(bonusCoin);
        delta.setTargetBonus(tb);
        result.setDelta(delta);
        return result;
    }

    private void insertCoinLedger(Long userId, int coinDelta, String type, Long relatedId, String description) {
        if (coinDelta == 0) return;
        PointsRecord pr = new PointsRecord();
        pr.setUserId(userId);
        pr.setPoints(coinDelta);
        pr.setType(type);
        pr.setRelatedId(relatedId);
        pr.setDescription(description);
        pr.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(pr);
    }

    @Data
    public static class TodayWaterSummary {
        private int currentWater;
        private LocalDateTime lastCheckinTime;
    }

    @Data
    public static class TargetBonus {
        private boolean triggered;
        private int exp;
        private int coin;
    }

    @Data
    public static class Delta {
        private int waterMl;
        private int exp;
        private int coin;
        private TargetBonus targetBonus;
    }

    @Data
    public static class CheckinSettlementResult {
        private CheckinRecord record;

        private Delta delta;

        private int level;
        private int exp;
        private int nextLevelExp;
        private int coin;
        private String stage;
        private boolean leveledUp;

        private int currentWater;
        private int targetWater;
    }
}


