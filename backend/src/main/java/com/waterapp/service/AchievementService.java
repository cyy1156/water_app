package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterapp.entity.*;
import com.waterapp.mapper.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成就服务
 * - 连续打卡成就计算
 * - 成就列表（系统成就 + 兑换成就）
 */
@Slf4j
@Service
public class AchievementService {

    @Autowired
    private AchievementMapper achievementMapper;
    @Autowired
    private AchievementConfigMapper achievementConfigMapper;
    @Autowired
    private CheckinRecordMapper checkinRecordMapper;
    @Autowired
    private UserMapper userMapper;

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 计算用户当前连续打卡天数（含今日）
     */
    public int getContinuousCheckinDays(Long userId) {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = checkinRecordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .select(CheckinRecord::getCheckinDate)
        ).stream().map(CheckinRecord::getCheckinDate).filter(Objects::nonNull).distinct().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        if (dates.isEmpty()) return 0;
        int streak = 0;
        LocalDate expect = today;
        for (LocalDate d : dates) {
            if (d.equals(expect)) {
                streak++;
                expect = expect.minusDays(1);
            } else if (d.isBefore(expect)) {
                break;
            }
        }
        return streak;
    }

    /**
     * 打卡后检查并发放连续打卡成就
     */
    @Transactional
    public void checkAndGrantStreakAchievements(Long userId) {
        int streak = getContinuousCheckinDays(userId);
        if (streak <= 0) return;
        List<AchievementConfig> configs = achievementConfigMapper.selectList(
                new LambdaQueryWrapper<AchievementConfig>().orderByAsc(AchievementConfig::getStreakDays)
        );
        for (AchievementConfig cfg : configs) {
            if (streak < cfg.getStreakDays()) continue;
            String streakPattern = "\"streakDays\":" + cfg.getStreakDays();
            long exist = achievementMapper.selectCount(
                    new LambdaQueryWrapper<Achievement>()
                            .eq(Achievement::getUserId, userId)
                            .eq(Achievement::getType, "SYSTEM")
                            .and(w -> w.like(Achievement::getConfigJson, "%" + streakPattern + ",%")
                                    .or().like(Achievement::getConfigJson, "%" + streakPattern + "}"))
            );
            if (exist > 0) continue;
            Map<String, Object> config = new HashMap<>();
            config.put("streakDays", cfg.getStreakDays());
            config.put("badgeName", cfg.getBadgeName());
            config.put("coinReward", cfg.getCoinReward() != null ? cfg.getCoinReward() : 0);
            try {
                String json = JSON.writeValueAsString(config);
                Achievement a = new Achievement();
                a.setUserId(userId);
                a.setType("SYSTEM");
                a.setConfigJson(json);
                a.setCompletedAt(LocalDateTime.now());
                achievementMapper.insert(a);
                int coin = cfg.getCoinReward() != null ? cfg.getCoinReward() : 0;
                if (coin > 0) {
                    User user = userMapper.selectById(userId);
                    if (user != null) {
                        int c = user.getCoin() != null ? user.getCoin() : 0;
                        user.setCoin(c + coin);
                        user.setTotalPoints(c + coin);
                        user.setUpdateTime(LocalDateTime.now());
                        userMapper.updateById(user);
                    }
                }
                log.info("成就发放 userId={}, streak={}, config={}", userId, streak, cfg.getBadgeName());
            } catch (Exception e) {
                log.error("成就发放失败", e);
            }
        }
    }

    /**
     * 获取用户成就列表（系统 + 兑换）
     */
    public AchievementListVO list(Long userId) {
        List<Achievement> list = achievementMapper.selectList(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getUserId, userId)
                        .orderByDesc(Achievement::getCompletedAt)
                        .orderByDesc(Achievement::getCreateTime)
        );
        List<AchievementItemVO> system = new ArrayList<>();
        List<AchievementItemVO> redeemed = new ArrayList<>();
        for (Achievement a : list) {
            AchievementItemVO vo = toItemVO(a);
            if ("SYSTEM".equals(a.getType())) system.add(vo);
            else redeemed.add(vo);
        }
        AchievementListVO result = new AchievementListVO();
        result.setSystemAchievements(system);
        result.setRedeemedAchievements(redeemed);
        return result;
    }

    @SuppressWarnings("unchecked")
    private AchievementItemVO toItemVO(Achievement a) {
        AchievementItemVO vo = new AchievementItemVO();
        vo.setId(a.getId());
        vo.setType(a.getType());
        vo.setConfigJson(a.getConfigJson());
        vo.setCompletedAt(a.getCompletedAt());
        vo.setExchangeRecordId(a.getExchangeRecordId());
        vo.setCreateTime(a.getCreateTime());
        try {
            if (a.getConfigJson() != null) {
                Map<String, Object> m = (Map<String, Object>) JSON.readValue(a.getConfigJson(), Map.class);
                vo.setBadgeName((String) m.get("badgeName"));
                vo.setStreakDays(m.get("streakDays") != null ? ((Number) m.get("streakDays")).intValue() : null);
                vo.setCoinReward(m.get("coinReward") != null ? ((Number) m.get("coinReward")).intValue() : null);
                vo.setGoodsName((String) m.get("goodsName"));
            }
        } catch (Exception ignored) {}
        return vo;
    }

    @Data
    public static class AchievementListVO {
        private List<AchievementItemVO> systemAchievements;
        private List<AchievementItemVO> redeemedAchievements;
    }

    @Data
    public static class AchievementItemVO {
        private Long id;
        private String type;
        private String configJson;
        private LocalDateTime completedAt;
        private Long exchangeRecordId;
        private LocalDateTime createTime;
        private String badgeName;
        private Integer streakDays;
        private Integer coinReward;
        private String goodsName;
    }
}
