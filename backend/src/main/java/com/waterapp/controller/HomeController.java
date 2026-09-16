package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.entity.User;
import com.waterapp.mapper.UserMapper;
import com.waterapp.service.GrowthService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 大升级：成长页接口
 */
@Slf4j
@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GrowthService growthService;

    /**
     * 成长页首屏聚合
     */
    @GetMapping("/summary")
    public Result<HomeSummaryVO> summary(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            int level = user.getLevel() == null ? 1 : user.getLevel();
            int exp = user.getExp() == null ? 0 : user.getExp();
            int nextLevelExp = growthService.nextLevelExp(level);
            int coin = user.getCoin() == null
                    ? (user.getTotalPoints() == null ? 0 : user.getTotalPoints())
                    : user.getCoin();
            String stage = user.getGrowthStage() != null ? user.getGrowthStage() : growthService.stageFromLevel(level);

            GrowthService.TodayWaterSummary today = growthService.getTodayWaterSummary(userId);
            int targetWater = user.getTargetWater() == null ? 2000 : user.getTargetWater();

            HomeSummaryVO vo = new HomeSummaryVO();
            UserSummary u = new UserSummary();
            u.setLevel(level);
            u.setExp(exp);
            u.setNextLevelExp(nextLevelExp);
            u.setCoin(coin);
            u.setStage(stage);
            vo.setUser(u);

            TodaySummary t = new TodaySummary();
            t.setCurrentWater(today.getCurrentWater());
            t.setTargetWater(targetWater);
            t.setProgress(targetWater > 0 ? (double) today.getCurrentWater() / (double) targetWater : 0d);
            t.setTargetReached(today.getCurrentWater() >= targetWater);
            vo.setToday(t);

            Map<String, Object> tips = new HashMap<>();
            tips.put("chatPlaceholder", "今天喝水了吗？💧");
            vo.setTips(tips);

            vo.setCoverImage(user.getCoverImage());
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取成长页概览失败", e);
            return Result.error("获取成长页概览失败：" + e.getMessage());
        }
    }

    /**
     * 喝水打卡结算（成长系统）
     * body: { "waterMl": 200, "waterType": "TEA" }  // waterType 可选：WATER/TEA/COFFEE/MILK_TEA/DRINK
     */
    @PostMapping("/checkin")
    public Result<HomeCheckinResultVO> checkin(HttpServletRequest request,
                                               @RequestBody CheckinBody body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            int waterMl = body == null || body.getWaterMl() == null ? 0 : body.getWaterMl();
            String waterType = body != null ? body.getWaterType() : null;
            GrowthService.CheckinSettlementResult r = growthService.checkinAndSettle(userId, waterMl, waterType);

            HomeCheckinResultVO vo = new HomeCheckinResultVO();
            vo.setDelta(r.getDelta());

            UserSummary u = new UserSummary();
            u.setLevel(r.getLevel());
            u.setExp(r.getExp());
            u.setNextLevelExp(r.getNextLevelExp());
            u.setCoin(r.getCoin());
            u.setStage(r.getStage());
            u.setLeveledUp(r.isLeveledUp());
            vo.setUser(u);

            TodaySummary t = new TodaySummary();
            t.setCurrentWater(r.getCurrentWater());
            t.setTargetWater(r.getTargetWater());
            t.setProgress(r.getTargetWater() > 0 ? (double) r.getCurrentWater() / (double) r.getTargetWater() : 0d);
            t.setTargetReached(r.getCurrentWater() >= r.getTargetWater());
            vo.setToday(t);

            return Result.success(vo);
        } catch (Exception e) {
            log.error("成长打卡结算失败", e);
            return Result.error("打卡失败：" + e.getMessage());
        }
    }

    @Data
    public static class CheckinBody {
        private Integer waterMl;
        /** M5：喝水品种 WATER/TEA/COFFEE/MILK_TEA/DRINK */
        private String waterType;
    }

    @Data
    public static class UserSummary {
        private Integer level;
        private Integer exp;
        private Integer nextLevelExp;
        private Integer coin;
        private String stage;
        private Boolean leveledUp;
    }

    @Data
    public static class TodaySummary {
        private Integer currentWater;
        private Integer targetWater;
        private Double progress;
        private Boolean targetReached;
    }

    @Data
    public static class HomeSummaryVO {
        private UserSummary user;
        private TodaySummary today;
        private Map<String, Object> tips;
        private String coverImage;
    }

    @Data
    public static class HomeCheckinResultVO {
        private GrowthService.Delta delta;
        private UserSummary user;
        private TodaySummary today;
    }
}


