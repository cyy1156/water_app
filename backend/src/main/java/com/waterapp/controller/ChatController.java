package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.service.ChatService;
import com.waterapp.service.ChatService.ChatMessageVO;
import com.waterapp.service.ChatService.ChatSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI对话相关接口
 */
@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<ChatSessionVO> send(HttpServletRequest request,
                                      @RequestBody Map<String, String> body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String sessionId = body.get("sessionId");
            String content = body.get("content");
            String imageUrl = body.get("imageUrl");

            ChatSessionVO vo = chatService.sendMessage(userId, sessionId, content, imageUrl);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Result.error("发送消息失败：" + e.getMessage());
        }
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public Result<List<ChatMessageVO>> getHistory(HttpServletRequest request,
                                                   @RequestParam("sessionId") String sessionId,
                                                   @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<ChatMessageVO> history = chatService.getHistory(userId, sessionId, limit);
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取对话历史失败", e);
            return Result.error("获取对话历史失败：" + e.getMessage());
        }
    }

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile file) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String url = chatService.uploadImage(userId, file);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return Result.success(result);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return Result.error("上传图片失败：" + e.getMessage());
        }
    }
}

