package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waterapp.entity.CheckinRecord;
import com.waterapp.entity.User;
import com.waterapp.mapper.CheckinRecordMapper;
import com.waterapp.mapper.UserMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打卡相关业务
 */
@Service
public class CheckinService {

    @Autowired
    private CheckinRecordMapper checkinRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PointsService pointsService;

    /**
     * 获取今日打卡概览数据
     */
    public TodayCheckinVO getToday(Long userId) {
        // 查询用户，获取每日目标水量
        User user = userMapper.selectById(userId);
        Integer targetWater = (user != null && user.getTargetWater() != null)
                ? user.getTargetWater()
                : 2000;

        LocalDate today = LocalDate.now();

        // 查询今日所有打卡记录
        List<CheckinRecord> records = checkinRecordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .eq(CheckinRecord::getCheckinDate, today)
                        .orderByDesc(CheckinRecord::getCheckinTime)
        );

        int currentWater = 0;
        List<CheckinRecordItemVO> historyList = new ArrayList<>();
        for (CheckinRecord record : records) {
            currentWater += record.getWaterMl() != null ? record.getWaterMl() : 0;

            CheckinRecordItemVO item = new CheckinRecordItemVO();
            item.setId(record.getId());
            item.setWaterMl(record.getWaterMl());
            item.setCheckinTime(record.getCheckinTime());
            historyList.add(item);
        }

        TodayCheckinVO vo = new TodayCheckinVO();
        vo.setCurrentWater(currentWater);
        vo.setTargetWater(targetWater);
        vo.setHistoryList(historyList);

        // 为了降低成本，线上环境先不使用 Redis 缓存，直接返回数据库计算结果
        return vo;
    }

    /**
     * 新增一条打卡记录
     */
    public CheckinRecord addRecord(Long userId, Integer waterMl) {
        LocalDateTime now = LocalDateTime.now();

        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setWaterMl(waterMl);
        record.setCheckinTime(now);
        record.setCheckinDate(now.toLocalDate());
        record.setCreateTime(now);

        checkinRecordMapper.insert(record);

        // 计算并记录积分
        int earnedPoints = pointsService.addPointsForCheckin(userId, record.getId(), waterMl);
        record.setPointsEarned(earnedPoints);
        // 更新记录中的积分字段
        checkinRecordMapper.updateById(record);

        return record;
    }

    /**
     * 指定日期的打卡历史列表
     */
    public List<HistoryItemVO> getHistory(Long userId, LocalDate date) {
        List<CheckinRecord> records = checkinRecordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .eq(CheckinRecord::getCheckinDate, date)
                        .orderByDesc(CheckinRecord::getCheckinTime)
        );

        List<HistoryItemVO> list = new ArrayList<>();
        for (CheckinRecord record : records) {
            HistoryItemVO vo = new HistoryItemVO();
            vo.setId(record.getId());
            vo.setWaterMl(record.getWaterMl());
            vo.setCheckinTime(record.getCheckinTime());
            vo.setWaterType(record.getWaterType());
            list.add(vo);
        }
        return list;
    }

    /**
     * 某年某月的打卡日历数据
     */
    public List<CalendarDayVO> getCalendar(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<CheckinRecord> records = checkinRecordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .between(CheckinRecord::getCheckinDate, start, end)
        );

        Map<LocalDate, Boolean> checkedMap = new HashMap<>();
        for (CheckinRecord record : records) {
            if (record.getCheckinDate() != null) {
                checkedMap.put(record.getCheckinDate(), Boolean.TRUE);
            }
        }

        List<CalendarDayVO> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            CalendarDayVO vo = new CalendarDayVO();
            vo.setDate(d);
            vo.setChecked(Boolean.TRUE.equals(checkedMap.get(d)));
            result.add(vo);
        }

        // 为了节省成本，这里暂时不使用 Redis 缓存，直接返回当月日历数据
        return result;
    }

    /**
     * M5：获取某月日历统计（每日打卡次数、总水量、品种分布）
     */
    public CalendarStatsVO getCalendarStats(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<CheckinRecord> records = checkinRecordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .between(CheckinRecord::getCheckinDate, start, end)
        );

        Map<LocalDate, DailyStat> statMap = new HashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DailyStat ds = new DailyStat();
            ds.setDate(d.toString());
            ds.setCheckinCount(0);
            ds.setTotalWater(0);
            ds.setWaterTypes(new HashMap<>());
            statMap.put(d, ds);
        }

        for (CheckinRecord r : records) {
            if (r.getCheckinDate() == null) continue;
            DailyStat ds = statMap.get(r.getCheckinDate());
            if (ds == null) continue;
            ds.setCheckinCount(ds.getCheckinCount() + 1);
            int ml = r.getWaterMl() != null ? r.getWaterMl() : 0;
            ds.setTotalWater(ds.getTotalWater() + ml);
            String wt = (r.getWaterType() != null && !r.getWaterType().isEmpty()) ? r.getWaterType() : "WATER";
            ds.getWaterTypes().merge(wt, 1, Integer::sum);
        }

        List<DailyStat> dailyStats = new ArrayList<>();
        int totalWater = 0;
        int activeDays = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DailyStat ds = statMap.get(d);
            if (ds != null) {
                dailyStats.add(ds);
                totalWater += ds.getTotalWater();
                if (ds.getCheckinCount() > 0) activeDays++;
            }
        }

        CalendarStatsVO vo = new CalendarStatsVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setDailyStats(dailyStats);
        MonthSummary ms = new MonthSummary();
        ms.setTotalDays(ym.lengthOfMonth());
        ms.setActiveDays(activeDays);
        ms.setTotalWater(totalWater);
        ms.setAvgWaterPerDay(activeDays > 0 ? totalWater / activeDays : 0);
        vo.setMonthSummary(ms);
        return vo;
    }

    private String buildTodayCacheKey(Long userId) {
        return "checkin:today:" + userId;
    }

    private String buildCalendarCacheKey(Long userId, int year, int month) {
        return "checkin:calendar:" + userId + ":" + year + "-" + month;
    }

    /**
     * 今日打卡概览 VO
     */
    @Data
    public static class TodayCheckinVO {
        private Integer currentWater;
        private Integer targetWater;
        private List<CheckinRecordItemVO> historyList;
    }

    /**
     * 打卡记录项 VO
     */
    @Data
    public static class CheckinRecordItemVO {
        private Long id;
        private Integer waterMl;
        private LocalDateTime checkinTime;
    }

    /**
     * 指定日期的打卡记录 VO（用于历史列表）
     */
    @Data
    public static class HistoryItemVO {
        private Long id;
        private Integer waterMl;
        private LocalDateTime checkinTime;
        private String waterType;
    }

    /**
     * 日历数据 VO
     */
    @Data
    public static class CalendarDayVO {
        /**
         * 日期（只保留到日）
         */
        private LocalDate date;

        /**
         * 是否有打卡记录
         */
        private Boolean checked;
    }

    /** M5：日历统计 VO */
    @Data
    public static class CalendarStatsVO {
        private Integer year;
        private Integer month;
        private List<DailyStat> dailyStats;
        private MonthSummary monthSummary;
    }

    @Data
    public static class DailyStat {
        private String date;
        private Integer checkinCount;
        private Integer totalWater;
        private Map<String, Integer> waterTypes;
    }

    @Data
    public static class MonthSummary {
        private Integer totalDays;
        private Integer activeDays;
        private Integer totalWater;
        private Integer avgWaterPerDay;
    }
}


