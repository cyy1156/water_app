package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.service.ReminderService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * AI 提醒相关接口（M4：粥粥喝水提醒）
 */
@Slf4j
@RestController
@RequestMapping("/ai/reminder")
public class AiReminderController {

    @Autowired
    private ReminderService reminderService;

    /**
     * 获取提醒设置
     * GET /ai/reminder/settings
     */
    @GetMapping("/settings")
    public Result<ReminderService.ReminderSettingsVO> getSettings(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            ReminderService.ReminderSettingsVO vo = reminderService.getSettings(userId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取提醒设置失败", e);
            return Result.error("获取提醒设置失败：" + e.getMessage());
        }
    }

    /**
     * 保存提醒设置
     * POST /ai/reminder/settings
     * body: { "enabled": true, "startTime": "09:00", "endTime": "22:00", "intervalMin": 90, "intensity": "SOFT", "quietAfterTarget": true }
     */
    @PostMapping("/settings")
    public Result<Void> saveSettings(HttpServletRequest request, @RequestBody SettingsBody body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            ReminderService.ReminderSettingsVO vo = new ReminderService.ReminderSettingsVO();
            vo.setEnabled(body.getEnabled() != null && body.getEnabled());
            vo.setStartTime(body.getStartTime());
            vo.setEndTime(body.getEndTime());
            vo.setIntervalMin(body.getIntervalMin());
            vo.setIntensity(body.getIntensity());
            vo.setQuietAfterTarget(body.getQuietAfterTarget() == null || body.getQuietAfterTarget());
            reminderService.saveSettings(userId, vo);
            return Result.success();
        } catch (Exception e) {
            log.error("保存提醒设置失败", e);
            return Result.error("保存提醒设置失败：" + e.getMessage());
        }
    }

    /**
     * 提交订阅授权结果（前端调用 wx.requestSubscribeMessage 后调用）
     * POST /ai/reminder/subscribe
     * body: { "accepted": true }
     */
    @PostMapping("/subscribe")
    public Result<Void> subscribe(HttpServletRequest request, @RequestBody SubscribeBody body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            boolean accepted = body.getAccepted() != null && body.getAccepted();
            reminderService.saveSubscribeResult(userId, accepted);
            return Result.success();
        } catch (Exception e) {
            log.error("提交订阅授权失败", e);
            return Result.error("提交订阅授权失败：" + e.getMessage());
        }
    }

    /**
     * 获取订阅消息模板 ID（用于前端 wx.requestSubscribeMessage）
     * GET /ai/reminder/template-id
     */
    @GetMapping("/template-id")
    public Result<Map<String, String>> getTemplateId(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            return Result.success(reminderService.getSubscribeTemplateId());
        } catch (Exception e) {
            log.error("获取模板ID失败", e);
            return Result.error("获取模板ID失败：" + e.getMessage());
        }
    }

    /**
     * 一次性订阅：用户授权成功后，立即发送本次提醒
     * POST /ai/reminder/send-now
     * 前端流程：wx.requestSubscribeMessage 用户同意 → 调用本接口 → 后端发送订阅消息
     */
    @PostMapping("/send-now")
    public Result<Boolean> sendNow(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            boolean ok = reminderService.sendRemindNow(userId);
            return Result.success(ok);
        } catch (Exception e) {
            log.error("发送提醒失败", e);
            return Result.error("发送提醒失败：" + e.getMessage());
        }
    }

    /**
     * 获取提醒预览（未完成项清单 - 任务2）
     * GET /ai/reminder/preview
     * 返回当前是否会触发提醒及预览文案，供设置页展示
     */
    @GetMapping("/preview")
    public Result<ReminderService.ReminderPreviewVO> getPreview(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            ReminderService.ReminderPreviewVO vo = reminderService.getPreview(userId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取提醒预览失败", e);
            return Result.error("获取提醒预览失败：" + e.getMessage());
        }
    }

    @Data
    public static class SettingsBody {
        private Boolean enabled;
        private String startTime;
        private String endTime;
        private Integer intervalMin;
        private String intensity;
        private Boolean quietAfterTarget;
    }

    @Data
    public static class SubscribeBody {
        private Boolean accepted;
    }
}

