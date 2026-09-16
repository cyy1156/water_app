package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waterapp.common.exception.BusinessException;
import com.waterapp.entity.ChatRecord;
import com.waterapp.mapper.ChatRecordMapper;
import com.waterapp.util.AiServiceUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.waterapp.vo.UserVO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * AI 对话相关业务
 */
@Slf4j
@Service
public class ChatService {

    @Value("${app.chat.max-history:50}")
    private int maxHistory;

    @Value("${app.chat.upload-dir:uploads/chat}")
    private String uploadDir;

    @Autowired
    private ChatRecordMapper chatRecordMapper;

    @Autowired
    private AiServiceUtil aiServiceUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private PointsService pointsService;

    /**
     * 发送一条消息，并获取 AI 回复
     */
    public ChatSessionVO sendMessage(Long userId, String sessionId, String content, String imageUrl) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 保存用户消息
        ChatRecord userRecord = new ChatRecord();
        userRecord.setUserId(userId);
        userRecord.setRole("user");
        userRecord.setContent(content);
        userRecord.setImageUrl(imageUrl);
        userRecord.setSessionId(sessionId);
        userRecord.setCreateTime(now);
        chatRecordMapper.insert(userRecord);

        // 2. 获取用户上下文数据
        UserContext context = buildUserContext(userId);

        // 3. 调用 AI 服务，获取回复内容（传入用户上下文）
        String aiReply = aiServiceUtil.callAiWithContext(content, context);

        // 3. 保存 AI 回复
        ChatRecord aiRecord = new ChatRecord();
        aiRecord.setUserId(userId);
        aiRecord.setRole("assistant");
        aiRecord.setContent(aiReply);
        aiRecord.setImageUrl(null);
        aiRecord.setSessionId(sessionId);
        aiRecord.setCreateTime(LocalDateTime.now());
        chatRecordMapper.insert(aiRecord);

        // 4. 组合返回结果
        List<ChatMessageVO> messages = new ArrayList<>();
        messages.add(toVO(userRecord));
        messages.add(toVO(aiRecord));

        ChatSessionVO vo = new ChatSessionVO();
        vo.setSessionId(sessionId);
        vo.setMessages(messages);
        return vo;
    }

    /**
     * 获取某个会话的最近消息历史
     */
    public List<ChatMessageVO> getHistory(Long userId, String sessionId, Integer limit) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new BusinessException("sessionId 不能为空");
        }

        int size = (limit == null || limit <= 0 || limit > maxHistory) ? maxHistory : limit;

        LambdaQueryWrapper<ChatRecord> wrapper = new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getUserId, userId)
                .eq(ChatRecord::getSessionId, sessionId)
                .orderByDesc(ChatRecord::getCreateTime)
                .last("LIMIT " + size);

        List<ChatRecord> records = chatRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        Collections.reverse(records);

        List<ChatMessageVO> result = new ArrayList<>();
        for (ChatRecord record : records) {
            result.add(toVO(record));
        }
        return result;
    }

    /**
     * 上传聊天图片，返回可访问的 URL
     */
    public String uploadImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = "chat_" + userId + "_" + System.currentTimeMillis() + ext;

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);

            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());

            return "/uploads/chat/" + filename;
        } catch (IOException e) {
            log.error("保存聊天图片失败", e);
            throw new BusinessException("图片上传失败，请稍后重试");
        }
    }

    private ChatMessageVO toVO(ChatRecord record) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(record.getId());
        vo.setRole(record.getRole());
        vo.setContent(record.getContent());
        vo.setImageUrl(record.getImageUrl());
        vo.setCreateTime(record.getCreateTime());
        vo.setSessionId(record.getSessionId());
        return vo;
    }

    @Data
    public static class ChatMessageVO {
        private Long id;
        private String role;
        private String content;
        private String imageUrl;
        private String sessionId;
        private LocalDateTime createTime;
    }

    @Data
    public static class ChatSessionVO {
        private String sessionId;
        private List<ChatMessageVO> messages;
    }

    /**
     * 构建用户上下文数据
     */
    private UserContext buildUserContext(Long userId) {
        UserContext context = new UserContext();
        context.setUserId(userId);

        try {
            // 获取用户基本信息
            UserVO userInfo = userService.getUserInfo(userId);
            if (userInfo != null) {
                context.setNickname(userInfo.getNickname());
                context.setTargetWater(userInfo.getTargetWater());
            }

            // 获取今日打卡数据
            CheckinService.TodayCheckinVO todayData = checkinService.getToday(userId);
            if (todayData != null) {
                context.setTodayWater(todayData.getCurrentWater());
                context.setTodayTarget(todayData.getTargetWater());
            }

            // 获取积分统计
            PointsService.PointsStatisticsVO pointsStats = pointsService.getStatistics(userId);
            if (pointsStats != null) {
                context.setTotalPoints(pointsStats.getTotalPoints());
            }

            // 获取最近7天的打卡记录（用于分析习惯）
            LocalDate today = LocalDate.now();
            List<String> recentCheckins = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate date = today.minusDays(i);
                List<CheckinService.HistoryItemVO> history = checkinService.getHistory(userId, date);
                if (history != null && !history.isEmpty()) {
                    int totalWater = history.stream()
                            .mapToInt(item -> item.getWaterMl() != null ? item.getWaterMl() : 0)
                            .sum();
                    if (totalWater > 0) {
                        recentCheckins.add(date.toString() + ": " + totalWater + "ml");
                    }
                }
            }
            context.setRecentCheckins(recentCheckins);

        } catch (Exception e) {
            log.warn("获取用户上下文数据失败，将使用空上下文: {}", e.getMessage());
        }

        return context;
    }

    /**
     * 用户上下文数据
     */
    @Data
    public static class UserContext {
        private Long userId;
        private String nickname;
        private Integer targetWater;      // 每日目标水量
        private Integer todayWater;       // 今日已喝水量
        private Integer todayTarget;      // 今日目标水量
        private Integer totalPoints;      // 总积分
        private List<String> recentCheckins;  // 最近打卡记录
    }
}

