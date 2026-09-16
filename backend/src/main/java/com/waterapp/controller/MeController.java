package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.entity.User;
import com.waterapp.mapper.UserMapper;
import com.waterapp.service.GrowthService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 大升级：我的页聚合接口
 */
@Slf4j
@RestController
@RequestMapping("/me")
public class MeController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GrowthService growthService;

    @GetMapping("/summary")
    public Result<MeSummaryVO> summary(HttpServletRequest request) {
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

            MeSummaryVO vo = new MeSummaryVO();
            vo.setNickname(user.getNickname());
            vo.setAvatarUrl(user.getAvatarUrl());
            vo.setCoverImage(user.getCoverImage());
            vo.setCoverText(user.getCoverText());
            vo.setLevel(level);
            vo.setExp(exp);
            vo.setNextLevelExp(nextLevelExp);
            vo.setCoin(coin);
            vo.setStage(stage);
            vo.setTargetWater(user.getTargetWater() != null ? user.getTargetWater() : 2000);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取我的页概览失败", e);
            return Result.error("获取我的页概览失败：" + e.getMessage());
        }
    }

    @Data
    public static class MeSummaryVO {
        private String nickname;
        private String avatarUrl;
        private String coverImage;
        private String coverText;

        private Integer level;
        private Integer exp;
        private Integer nextLevelExp;
        private Integer coin;
        private String stage;
        private Integer targetWater;
    }
}


