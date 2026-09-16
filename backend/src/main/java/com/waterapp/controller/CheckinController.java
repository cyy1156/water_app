package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.entity.CheckinRecord;
import com.waterapp.service.CheckinService;
import com.waterapp.service.CheckinService.TodayCheckinVO;
import com.waterapp.service.CheckinService.HistoryItemVO;
import com.waterapp.service.CheckinService.CalendarDayVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 打卡相关接口
 */
@Slf4j
@RestController
@RequestMapping("/checkin")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;

    /**
     * 获取今日打卡概览数据
     */
    @GetMapping("/today")
    public Result<TodayCheckinVO> getToday(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            TodayCheckinVO vo = checkinService.getToday(userId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取今日打卡数据失败", e);
            return Result.error("获取今日打卡数据失败：" + e.getMessage());
        }
    }

    /**
     * 新增一条打卡记录
     */
    @PostMapping("/record")
    public Result<CheckinRecord> addRecord(HttpServletRequest request,
                                           @RequestBody Map<String, Integer> body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Integer waterMl = body.get("waterMl");
            if (waterMl == null || waterMl <= 0) {
                return Result.badRequest("喝水量必须大于0");
            }
            CheckinRecord record = checkinService.addRecord(userId, waterMl);
            return Result.success(record);
        } catch (Exception e) {
            log.error("新增打卡记录失败", e);
            return Result.error("新增打卡记录失败：" + e.getMessage());
        }
    }

    /**
     * 指定日期的打卡历史
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getHistory(HttpServletRequest request,
                                                        @RequestParam("date") String dateStr) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            LocalDate date = LocalDate.parse(dateStr);
            List<HistoryItemVO> list = checkinService.getHistory(userId, date);

            // 转换为前端需要的结构：id、time(字符串)、ml、waterType
            List<Map<String, Object>> result = list.stream().map(item -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", item.getId());
                map.put("time", item.getCheckinTime());
                map.put("ml", item.getWaterMl());
                map.put("waterType", item.getWaterType());
                return map;
            }).collect(java.util.stream.Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取打卡历史失败", e);
            return Result.error("获取打卡历史失败：" + e.getMessage());
        }
    }

    /**
     * 获取某月日历打卡数据
     */
    @GetMapping("/calendar")
    public Result<List<Map<String, Object>>> getCalendar(HttpServletRequest request,
                                                         @RequestParam("year") Integer year,
                                                         @RequestParam("month") Integer month) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<CalendarDayVO> list = checkinService.getCalendar(userId, year, month);

            // 转换为前端需要的结构：date(yyyy-MM-dd)、checked
            List<Map<String, Object>> result = list.stream().map(item -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("date", item.getDate().toString());
                map.put("checked", item.getChecked());
                return map;
            }).collect(java.util.stream.Collectors.toList());

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取日历数据失败", e);
            return Result.error("获取日历数据失败：" + e.getMessage());
        }
    }
}


