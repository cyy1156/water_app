package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.service.CheckinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * M5：日历统计接口
 */
@Slf4j
@RestController
@RequestMapping("/calendar")
public class CalendarController {

    @Autowired
    private CheckinService checkinService;

    /**
     * 获取某月日历统计（每日打卡次数、总水量、品种分布）
     * GET /calendar/stats?year=2024&month=1
     */
    @GetMapping("/stats")
    public Result<CheckinService.CalendarStatsVO> stats(HttpServletRequest request,
                                                        @RequestParam("year") Integer year,
                                                        @RequestParam("month") Integer month) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            if (year == null || month == null || month < 1 || month > 12) {
                return Result.badRequest("year 和 month 参数无效");
            }
            CheckinService.CalendarStatsVO vo = checkinService.getCalendarStats(userId, year, month);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取日历统计失败", e);
            return Result.error("获取日历统计失败：" + e.getMessage());
        }
    }
}

