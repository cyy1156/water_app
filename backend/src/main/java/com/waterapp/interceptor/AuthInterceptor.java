package com.waterapp.interceptor;

import com.waterapp.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 认证拦截器
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 开发调试兜底：无 token 时是否允许使用测试用户
     * - 本地 application.yml 建议 true（方便调试）
     * - 云端 application-cloud.yml 必须 false（否则所有人都变成 userId=1，订阅/头像都会错乱）
     */
    @Value("${app.auth.dev-bypass:true}")
    private boolean devBypass;

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // 放行OPTIONS请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取Token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);

            // 验证Token
            if (jwtUtil.validateToken(token)) {
                // 将用户ID存入request，供Controller使用
                Long userId = jwtUtil.getUserIdFromToken(token);
                request.setAttribute("userId", userId);
                return true;
            }
        }

        if (devBypass) {
            // ===== 开发兼容：如果没有携带 Token，则使用测试用户ID=1 =====
            log.warn("请求未携带有效Token，临时使用测试用户 userId=1（仅用于开发调试）");
            request.setAttribute("userId", 1L);
            return true;
        }

        // 云端/生产：严格要求 token（保持与 Result 结构一致：HTTP 200 + 业务 code=401）
        try {
            response.setStatus(200);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权，请先登录\",\"data\":null}");
            response.getWriter().flush();
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
}

