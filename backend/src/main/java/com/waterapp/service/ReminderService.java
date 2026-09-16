package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waterapp.entity.ReminderLog;
import com.waterapp.entity.User;
import com.waterapp.entity.UserReminderSetting;
import com.waterapp.mapper.ReminderLogMapper;
import com.waterapp.mapper.UserMapper;
import com.waterapp.mapper.UserReminderSettingMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 粥粥提醒服务（M4）
 * - 用户提醒设置 CRUD
 * - 一次性订阅：用户授权后立即发送提醒消息
 */
@Slf4j
@Service
public class ReminderService {

    @Autowired
    private UserReminderSettingMapper reminderSettingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GrowthService growthService;

    @Autowired
    private WechatSubscribeService wechatSubscribeService;

    @Autowired
    private ReminderLogMapper reminderLogMapper;

    @Value("${wechat.subscribe.template-id.remind:}")
    private String remindTemplateId;

    /**
     * 获取用户提醒设置（不存在则返回默认）
     */
    public ReminderSettingsVO getSettings(Long userId) {
        UserReminderSetting entity = reminderSettingMapper.selectById(userId);
        ReminderSettingsVO vo = new ReminderSettingsVO();
        if (entity != null) {
            vo.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
            vo.setStartTime(entity.getStartTime() != null ? entity.getStartTime().toString() : "09:00");
            vo.setEndTime(entity.getEndTime() != null ? entity.getEndTime().toString() : "22:00");
            vo.setIntervalMin(entity.getIntervalMin() != null ? entity.getIntervalMin() : 90);
            vo.setIntensity(entity.getIntensity() != null ? entity.getIntensity() : "SOFT");
            vo.setQuietAfterTarget(entity.getQuietAfterTarget() == null || entity.getQuietAfterTarget() == 1);
        } else {
            vo.setEnabled(false);
            vo.setStartTime("09:00");
            vo.setEndTime("22:00");
            vo.setIntervalMin(90);
            vo.setIntensity("SOFT");
            vo.setQuietAfterTarget(true);
        }
        boolean hasTemplate = remindTemplateId != null && !remindTemplateId.trim().isEmpty();
        vo.setTemplateIdAvailable(hasTemplate);
        vo.setTemplateId(hasTemplate ? remindTemplateId.trim() : null);
        return vo;
    }

    /**
     * 保存用户提醒设置
     */
    public void saveSettings(Long userId, ReminderSettingsVO vo) {
        UserReminderSetting entity = reminderSettingMapper.selectById(userId);
        if (entity == null) {
            entity = new UserReminderSetting();
            entity.setUserId(userId);
        }
        entity.setEnabled(vo.isEnabled() ? 1 : 0);
        entity.setStartTime(parseTime(vo.getStartTime(), "09:00"));
        entity.setEndTime(parseTime(vo.getEndTime(), "22:00"));
        entity.setIntervalMin(vo.getIntervalMin() != null ? vo.getIntervalMin() : 90);
        entity.setIntensity(vo.getIntensity() != null ? vo.getIntensity() : "SOFT");
        entity.setQuietAfterTarget(vo.isQuietAfterTarget() ? 1 : 0);

        if (reminderSettingMapper.selectById(userId) != null) {
            reminderSettingMapper.updateById(entity);
        } else {
            reminderSettingMapper.insert(entity);
        }
        log.info("用户提醒设置已保存 userId={}, enabled={}", userId, entity.getEnabled());
    }

    /**
     * 提交订阅授权结果（前端调用 wx.requestSubscribeMessage 后上报）
     */
    public void saveSubscribeResult(Long userId, boolean accepted) {
        UserReminderSetting entity = reminderSettingMapper.selectById(userId);
        if (entity == null) {
            entity = new UserReminderSetting();
            entity.setUserId(userId);
            // 默认设置仍然交给 saveSettings 控制，这里不直接开启/关闭
            entity.setEnabled(0);
            entity.setStartTime(LocalTime.of(9, 0));
            entity.setEndTime(LocalTime.of(22, 0));
            entity.setIntervalMin(90);
            entity.setIntensity("SOFT");
            entity.setQuietAfterTarget(1);
            reminderSettingMapper.insert(entity);
        }
        // 当前版本：订阅授权结果仅用于埋点/分析，不强制覆盖用户在前端开关里的选择
        log.info("用户订阅提醒授权结果 userId={}, accepted={}", userId, accepted);
    }

    /**
     * 获取订阅消息模板 ID（前端授权时需要）
     */
    public Map<String, String> getSubscribeTemplateId() {
        Map<String, String> map = new HashMap<>();
        map.put("templateId", remindTemplateId != null ? remindTemplateId : "");
        return map;
    }

    /**
     * 发送本次提醒（一次性订阅：前端请求授权成功后调用）
     * 根据用户今日喝水进度生成提醒文案并发送订阅消息
     * 模板字段：thing1(温馨提示)、thing5(目标饮水量)、time6(饮水时间)、phrase4(距离上次饮水)
     */
    public boolean sendRemindNow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getOpenid() == null || user.getOpenid().trim().isEmpty()) {
            log.warn("用户不存在或无 openid userId={}", userId);
            return false;
        }

        if (!wechatSubscribeService.isTemplateConfigured()) {
            log.warn("未配置订阅模板，无法发送");
            return false;
        }

        GrowthService.TodayWaterSummary today = growthService.getTodayWaterSummary(userId);
        int targetWater = user.getTargetWater() != null ? user.getTargetWater() : 2000;
        int currentWater = today.getCurrentWater();
        LocalDateTime lastCheckinTime = today.getLastCheckinTime();

        // thing1: 今日已喝水进度（最多20字）
        String thing1 = String.format("今日已喝水 %d/%dml", currentWater, targetWater);

        // thing5: 目标饮水量（最多20字，保留字段含义）
        String thing5 = targetWater + "ml";

        // time6: 饮水时间（格式：HH:mm）
        LocalDateTime now = LocalDateTime.now();
        String time6 = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // phrase4: 距离上次饮水（最多5字，使用固定短语，避免微信短语字典校验失败）
        String phrase4 = buildPhrase4(lastCheckinTime, now);

        boolean ok = wechatSubscribeService.sendRemindMessage(user.getOpenid(), thing1, thing5, time6, phrase4);
        if (ok) {
            log.info("喝水提醒已发送 userId={}, thing1={}, thing5={}, time6={}, phrase4={}", 
                    userId, thing1, thing5, time6, phrase4);
        }
        return ok;
    }

    /**
     * 获取提醒预览（未完成项清单 - 任务2）
     * 与定时任务使用相同规则判断 wouldSend，并生成与订阅消息一致的文案预览
     */
    public ReminderPreviewVO getPreview(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime nowTime = now.toLocalTime();
        ReminderPreviewVO vo = new ReminderPreviewVO();
        vo.setDetail(new ReminderPreviewVO.PreviewDetail());

        User user = userMapper.selectById(userId);
        int targetWater = user != null && user.getTargetWater() != null ? user.getTargetWater() : 2000;
        GrowthService.TodayWaterSummary today = growthService.getTodayWaterSummary(userId);
        LocalDateTime lastCheckinTime = today.getLastCheckinTime();

        int currentWater = today.getCurrentWater();

        // 始终生成与订阅消息一致的文案（供预览展示）
        String thing1 = String.format("今日已喝水 %d/%dml", currentWater, targetWater);
        String thing5 = targetWater + "ml";
        String time6 = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        String phrase4 = buildPhrase4(lastCheckinTime, now);
        vo.getDetail().setThing1(thing1);
        vo.getDetail().setThing5(thing5);
        vo.getDetail().setTime6(time6);
        vo.getDetail().setPhrase4(phrase4);
        vo.setPreviewText(String.format("今日已喝水 %d/%dml，上次喝水 %s。", currentWater, targetWater, phrase4));

        UserReminderSetting setting = reminderSettingMapper.selectById(userId);
        if (setting == null || setting.getEnabled() == null || setting.getEnabled() != 1) {
            vo.setWouldSend(false);
            vo.setReason("未开启提醒");
            return vo;
        }

        LocalTime startTime = setting.getStartTime() != null ? setting.getStartTime() : LocalTime.of(9, 0);
        LocalTime endTime = setting.getEndTime() != null ? setting.getEndTime() : LocalTime.of(22, 0);
        if (nowTime.isBefore(startTime) || nowTime.isAfter(endTime)) {
            vo.setWouldSend(false);
            vo.setReason(String.format("当前不在提醒时段内（%s-%s）", startTime, endTime));
            return vo;
        }

        int intervalMin = setting.getIntervalMin() != null ? setting.getIntervalMin() : 90;
        LocalDateTime sinceTime = now.minusMinutes(intervalMin);
        long recentCount = reminderLogMapper.selectCount(
                new LambdaQueryWrapper<ReminderLog>()
                        .eq(ReminderLog::getUserId, userId)
                        .eq(ReminderLog::getStatus, "SENT")
                        .ge(ReminderLog::getCreateTime, sinceTime)
        );
        if (recentCount > 0) {
            vo.setWouldSend(false);
            vo.setReason(String.format("%d 分钟内已发送过提醒，避免重复", intervalMin));
            return vo;
        }

        if (setting.getQuietAfterTarget() != null && setting.getQuietAfterTarget() == 1
                && today.getCurrentWater() >= targetWater) {
            vo.setWouldSend(false);
            vo.setReason("今日已达成目标且已开启达标静默");
            return vo;
        }

        if (lastCheckinTime != null) {
            long minutesSinceLast = java.time.Duration.between(lastCheckinTime, now).toMinutes();
            if (minutesSinceLast < 30) {
                vo.setWouldSend(false);
                vo.setReason(String.format("距离上次喝水仅 %d 分钟（满 30 分钟才会提醒）", minutesSinceLast));
                return vo;
            }
        }

        vo.setWouldSend(true);
        vo.setReason("当前会触发提醒");
        return vo;
    }

    private static String buildPhrase4(LocalDateTime lastCheckinTime, LocalDateTime now) {
        if (lastCheckinTime == null) return "首次";
        long minutes = java.time.Duration.between(lastCheckinTime, now).toMinutes();
        if (minutes < 15) {
            return "刚刚";
        }
        if (minutes < 45) {
            return "有一会了";
        }
        // 45 分钟及以上统一视为「已经好久啦」
        return "已经好久啦";
    }

    private LocalTime parseTime(String s, String defaultVal) {
        if (s == null || s.trim().isEmpty()) return parseTime(defaultVal, "09:00");
        try {
            String[] parts = s.split(":");
            int h = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 9;
            int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            return LocalTime.of(Math.min(23, Math.max(0, h)), Math.min(59, Math.max(0, m)));
        } catch (Exception e) {
            return LocalTime.of(9, 0);
        }
    }

    @Data
    public static class ReminderSettingsVO {
        private boolean enabled;
        private String startTime;
        private String endTime;
        private Integer intervalMin;
        private String intensity;
        private boolean quietAfterTarget;
        /** 是否已配置订阅模板（未配置时前端不弹授权） */
        private boolean templateIdAvailable;
        /** 模板 ID（用于 wx.requestSubscribeMessage，仅当 templateIdAvailable 时有效） */
        private String templateId;
    }

    /** 提醒预览 VO（任务2） */
    @Data
    public static class ReminderPreviewVO {
        private Boolean wouldSend;
        private String reason;
        private String previewText;
        private PreviewDetail detail;

        @Data
        public static class PreviewDetail {
            private String thing1;
            private String thing5;
            private String time6;
            private String phrase4;
        }
    }
}

