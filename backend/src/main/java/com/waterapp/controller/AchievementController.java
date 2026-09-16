package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.service.AchievementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 成就接口
 * GET /achievement/list 返回用户系统成就 + 兑换成就（双专栏）
 */
@Slf4j
@RestController
@RequestMapping("/achievement")
public class AchievementController {

    @Autowired
    private AchievementService achievementService;

    @GetMapping("/list")
    public Result<AchievementService.AchievementListVO> list(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            AchievementService.AchievementListVO vo = achievementService.list(userId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取成就列表失败", e);
            return Result.error("获取成就列表失败：" + e.getMessage());
        }
    }
}
