package com.waterapp.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waterapp.entity.ReminderLog;
import com.waterapp.entity.User;
import com.waterapp.entity.UserReminderSetting;
import com.waterapp.mapper.ReminderLogMapper;
import com.waterapp.mapper.UserMapper;
import com.waterapp.mapper.UserReminderSettingMapper;
import com.waterapp.service.GrowthService;
import com.waterapp.service.WechatSubscribeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 粥粥提醒定时任务（M4）
 * 每10分钟扫描一次，自动发送订阅消息提醒
 */
@Slf4j
@Component
public class ReminderScheduledTask {

    @Autowired
    private UserReminderSettingMapper reminderSettingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReminderLogMapper reminderLogMapper;

    @Autowired
    private GrowthService growthService;

    @Autowired
    private WechatSubscribeService wechatSubscribeService;

    @Value("${wechat.subscribe.template-id.remind:}")
    private String remindTemplateId;

    @Value("${reminder.scan-interval-min:10}")
    private int scanIntervalMin;

    /**
     * 定时扫描并发送提醒（每10分钟执行一次）
     * Cron表达式：0 0/10 * * * ? （每10分钟）
     */
    @Scheduled(cron = "${reminder.cron:0 0/10 * * * ?}")
    public void scanAndSendReminders() {
        if (remindTemplateId == null || remindTemplateId.trim().isEmpty()) {
            log.debug("未配置订阅模板ID，跳过扫描");
            return;
        }

        if (!wechatSubscribeService.isTemplateConfigured()) {
            log.debug("订阅模板未配置，跳过扫描");
            return;
        }

        try {
            LocalTime now = LocalTime.now();
            LocalDateTime nowDateTime = LocalDateTime.now();

            // 1. 查询所有开启提醒的用户
            List<UserReminderSetting> settings = reminderSettingMapper.selectList(
                    new LambdaQueryWrapper<UserReminderSetting>()
                            .eq(UserReminderSetting::getEnabled, 1)
            );

            log.debug("扫描到 {} 个开启提醒的用户", settings.size());

            int sentCount = 0;
            int skippedCount = 0;

            for (UserReminderSetting setting : settings) {
                try {
                    if (shouldSendReminder(setting, now, nowDateTime)) {
                        boolean sent = sendReminder(setting.getUserId(), nowDateTime);
                        if (sent) {
                            sentCount++;
                        } else {
                            skippedCount++;
                        }
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    log.error("处理用户提醒失败 userId={}", setting.getUserId(), e);
                    recordLog(setting.getUserId(), "REMIND_SOFT", null, "SUBSCRIBE_MSG", "FAILED");
                }
            }

            log.info("提醒扫描完成：发送={}, 跳过={}, 总计={}", sentCount, skippedCount, settings.size());
        } catch (Exception e) {
            log.error("提醒扫描任务异常", e);
        }
    }

    /**
     * 判断是否应该发送提醒
     */
    private boolean shouldSendReminder(UserReminderSetting setting, LocalTime now, LocalDateTime nowDateTime) {
        // 1. 检查时段：是否在提醒时段内
        LocalTime startTime = setting.getStartTime() != null ? setting.getStartTime() : LocalTime.of(9, 0);
        LocalTime endTime = setting.getEndTime() != null ? setting.getEndTime() : LocalTime.of(22, 0);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            log.debug("用户不在提醒时段内 userId={}, now={}, 时段={}-{}", 
                    setting.getUserId(), now, startTime, endTime);
            return false;
        }

        // 2. 检查防重复：interval_min 内是否已发送过
        int intervalMin = setting.getIntervalMin() != null ? setting.getIntervalMin() : 90;
        LocalDateTime sinceTime = nowDateTime.minusMinutes(intervalMin);
        long recentCount = reminderLogMapper.selectCount(
                new LambdaQueryWrapper<ReminderLog>()
                        .eq(ReminderLog::getUserId, setting.getUserId())
                        .eq(ReminderLog::getStatus, "SENT")
                        .ge(ReminderLog::getCreateTime, sinceTime)
        );
        if (recentCount > 0) {
            log.debug("用户 {} 在 {} 分钟内已发送过提醒，跳过", setting.getUserId(), intervalMin);
            return false;
        }

        // 3. 检查达标静默：如果已达标且开启静默，则跳过
        if (setting.getQuietAfterTarget() != null && setting.getQuietAfterTarget() == 1) {
            GrowthService.TodayWaterSummary today = growthService.getTodayWaterSummary(setting.getUserId());
            User user = userMapper.selectById(setting.getUserId());
            int targetWater = user != null && user.getTargetWater() != null ? user.getTargetWater() : 2000;
            if (today.getCurrentWater() >= targetWater) {
                log.debug("用户 {} 今日已达标且开启静默，跳过提醒", setting.getUserId());
                return false;
            }
        }

        // 4. 检查距离上次喝水时间（建议：超过30分钟才提醒）
        GrowthService.TodayWaterSummary today = growthService.getTodayWaterSummary(setting.getUserId());
        LocalDateTime lastCheckinTime = today.getLastCheckinTime();
        if (lastCheckinTime != null) {
            long minutesSinceLast = java.time.Duration.between(lastCheckinTime, nowDateTime).toMinutes();
            if (minutesSinceLast < 30) {
                log.debug("用户 {} 距离上次喝水仅 {} 分钟，跳过提醒", setting.getUserId(), minutesSinceLast);
                return false;
            }
        }

        return true;
    }

    /**
     * 发送提醒消息
     */
    private boolean sendReminder(Long userId, LocalDateTime now) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getOpenid() == null || user.getOpenid().trim().isEmpty()) {
            log.warn("用户不存在或无 openid userId={}", userId);
            recordLog(userId, "REMIND_SOFT", null, "SUBSCRIBE_MSG", "SKIPPED");
            return false;
        }

        GrowthService.TodayWaterSummary today = growthService.getTodayWaterSummary(userId);
        int targetWater = user.getTargetWater() != null ? user.getTargetWater() : 2000;
        LocalDateTime lastCheckinTime = today.getLastCheckinTime();

        // 生成消息内容
        String thing1 = "饮水时间到啦,请您记得适量饮水。";
        String thing5 = targetWater + "ml";
        String time6 = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        String phrase4 = "刚刚";
        if (lastCheckinTime != null) {
            long minutes = java.time.Duration.between(lastCheckinTime, now).toMinutes();
            if (minutes < 1) {
                phrase4 = "刚刚";
            } else if (minutes < 60) {
                phrase4 = minutes + "分钟";
            } else {
                long hours = minutes / 60;
                if (hours < 24) {
                    phrase4 = hours + "小时";
                } else {
                    long days = hours / 24;
                    phrase4 = days + "天";
                }
            }
        } else {
            phrase4 = "首次";
        }

        // 发送订阅消息
        boolean ok = wechatSubscribeService.sendRemindMessage(user.getOpenid(), thing1, thing5, time6, phrase4);
        
        // 记录日志
        String status = ok ? "SENT" : "FAILED";
        String content = String.format("提醒：目标%dml，距离上次%s", targetWater, phrase4);
        recordLog(userId, "REMIND_SOFT", content, "SUBSCRIBE_MSG", status);

        if (ok) {
            log.info("定时提醒发送成功 userId={}, thing1={}, thing5={}, time6={}, phrase4={}", 
                    userId, thing1, thing5, time6, phrase4);
        } else {
            log.warn("定时提醒发送失败 userId={}", userId);
        }

        return ok;
    }

    /**
     * 记录提醒日志
     */
    private void recordLog(Long userId, String scene, String content, String channel, String status) {
        try {
            ReminderLog record = new ReminderLog();
            record.setUserId(userId);
            record.setScene(scene);
            record.setContent(content);
            record.setChannel(channel);
            record.setStatus(status);
            record.setCreateTime(LocalDateTime.now());
            reminderLogMapper.insert(record);
        } catch (Exception e) {
            log.error("记录提醒日志失败 userId={}", userId, e);
        }
    }
}

