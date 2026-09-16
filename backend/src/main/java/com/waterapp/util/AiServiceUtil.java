package com.waterapp.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * AI 服务调用工具类
 * 支持 Ollama 本地部署
 */
@Slf4j
@Component
public class AiServiceUtil {

    @Value("${app.chat.ai-type:mock}")
    private String aiType;

    // Ollama 配置
    @Value("${app.chat.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${app.chat.ollama.model:qwen2.5:1.5b}")
    private String ollamaModel;

    @Value("${app.chat.ollama.timeout:30000}")
    private int ollamaTimeout;

    /**
     * 调用 AI 服务获取回复（带用户上下文）
     */
    public String callAiWithContext(String userMessage, Object userContext) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "我没有收到你的具体内容，可以再详细说说你的喝水情况吗？";
        }

        try {
            // 构建包含用户上下文的完整prompt
            String enhancedPrompt = buildEnhancedPrompt(userMessage, userContext);

            if ("ollama".equalsIgnoreCase(aiType)) {
                return callOllama(enhancedPrompt);
            } else {
                return callMock(userMessage);
            }
        } catch (Exception e) {
            log.error("调用 AI 服务失败，类型: {}, 错误: {}", aiType, e.getMessage(), e);
            return "抱歉，AI 服务暂时不可用，请稍后重试。错误信息：" + e.getMessage();
        }
    }

    /**
     * 调用 AI 服务获取回复（兼容旧接口）
     */
    public String callAi(String userMessage) {
        return callAiWithContext(userMessage, null);
    }

    /**
     * 构建增强的prompt，包含用户上下文
     */
    private String buildEnhancedPrompt(String userMessage, Object userContext) {
        if (userContext == null) {
            return userMessage;
        }

        try {
            // 使用反射获取用户上下文信息（避免直接依赖ChatService的内部类）
            java.lang.reflect.Method getNickname = userContext.getClass().getMethod("getNickname");
            java.lang.reflect.Method getTodayWater = userContext.getClass().getMethod("getTodayWater");
            java.lang.reflect.Method getTodayTarget = userContext.getClass().getMethod("getTodayTarget");
            java.lang.reflect.Method getTotalPoints = userContext.getClass().getMethod("getTotalPoints");
            java.lang.reflect.Method getRecentCheckins = userContext.getClass().getMethod("getRecentCheckins");

            String nickname = (String) getNickname.invoke(userContext);
            Integer todayWater = (Integer) getTodayWater.invoke(userContext);
            Integer todayTarget = (Integer) getTodayTarget.invoke(userContext);
            Integer totalPoints = (Integer) getTotalPoints.invoke(userContext);
            @SuppressWarnings("unchecked")
            List<String> recentCheckins = (List<String>) getRecentCheckins.invoke(userContext);

            StringBuilder contextInfo = new StringBuilder();
            contextInfo.append("【用户信息】\n");
            if (nickname != null) {
                contextInfo.append("昵称: ").append(nickname).append("\n");
            }
            if (todayWater != null && todayTarget != null) {
                contextInfo.append("今日喝水量: ").append(todayWater).append("ml / ").append(todayTarget).append("ml\n");
                int progress = (int) (todayWater * 100.0 / todayTarget);
                contextInfo.append("完成进度: ").append(progress).append("%\n");
            }
            if (totalPoints != null) {
                contextInfo.append("总积分: ").append(totalPoints).append("分\n");
            }
            if (recentCheckins != null && !recentCheckins.isEmpty()) {
                contextInfo.append("最近打卡记录:\n");
                for (String record : recentCheckins) {
                    contextInfo.append("  - ").append(record).append("\n");
                }
            }
            contextInfo.append("\n【用户问题】\n").append(userMessage);
            contextInfo.append("\n\n请根据以上用户信息，提供个性化的回答和建议。");

            return contextInfo.toString();
        } catch (Exception e) {
            log.warn("构建增强prompt失败，使用原始消息: {}", e.getMessage());
            return userMessage;
        }
    }

    /**
     * 调用 Ollama 本地 AI
     */
    private String callOllama(String userMessage) throws Exception {
        String url = ollamaBaseUrl + "/api/generate";
        
        JSONObject payload = new JSONObject();
        payload.put("model", ollamaModel);
        payload.put("prompt", userMessage);
        payload.put("stream", false);

        log.info("调用 Ollama API: url={}, model={}", url, ollamaModel);
        String response = sendHttpPost(url, payload.toJSONString(), null, ollamaTimeout);
        JSONObject json = JSON.parseObject(response);
        
        String reply = json.getString("response");
        if (reply == null || reply.trim().isEmpty()) {
            throw new Exception("Ollama 返回空回复");
        }
        
        log.info("Ollama 回复成功，长度: {}", reply.length());
        return reply.trim();
    }

    /**
     * 本地模拟 AI 回复（默认方案）
     */
    private String callMock(String userMessage) {
        String text = userMessage.trim().toLowerCase();
        
        if (text.contains("喝水") || text.contains("水") || text.contains("饮水")) {
            return "从你刚才的描述看，你已经在关注自己的喝水情况了，很棒！一般建议成年人每天饮水 1500-2000ml，" +
                    "可以在早起、上午、下午和晚上分几次喝，不要一次大量喝水。如果你愿意，我可以根据你的体重和作息帮你逐步养成喝水习惯。";
        }
        
        if (text.contains("头痛") || text.contains("头疼") || text.contains("不舒服") || text.contains("难受")) {
            return "出现这些不舒服的症状时，除了注意补充水分，也要留意是否休息不足、用眼过度或感冒等原因。" +
                    "如果症状持续或加重，一定要及时就医，听从专业医生的建议。这个小程序只能做日常健康陪伴，不能代替医疗诊断哦。";
        }
        
        return "我已经收到你的消息啦～目前我是一个简单的喝水健康陪伴助手，" +
                "可以帮你记录喝水、查看打卡情况和积分。你可以跟我说说你今天喝了多少水，或者有什么想养成的作息习惯。";
    }

    /**
     * 发送 HTTP POST 请求
     */
    private String sendHttpPost(String url, String jsonBody, String authHeader, int timeout) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        
        httpPost.setHeader("Content-Type", "application/json");
        if (authHeader != null && !authHeader.trim().isEmpty()) {
            httpPost.setHeader("Authorization", authHeader);
        }
        
        StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);
        
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity responseEntity = response.getEntity();
            String result = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new Exception("HTTP 请求失败，状态码: " + statusCode + ", 响应: " + result);
            }
            
            return result;
        } finally {
            httpClient.close();
        }
    }
}

