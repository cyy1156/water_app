package com.waterapp.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信订阅消息服务
 * - 获取 access_token（Redis 缓存 2 小时）
 * - 发送一次性订阅消息
 */
@Slf4j
@Service
public class WechatSubscribeService {

    private static final String REDIS_KEY_ACCESS_TOKEN = "wechat:access_token";
    private static final int TOKEN_EXPIRE_SECONDS = 7000; // 2h=7200，提前 200s 刷新

    // 在微信云托管环境中，通过开放接口服务访问微信开放平台，需使用 http://api.weixin.qq.com
    // 且在云托管控制台的「开放接口服务」中配置 cgi-bin/token 和 cgi-bin/message/subscribe/send 白名单
    private static final String TOKEN_URL = "http://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String SEND_URL = "http://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s";

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Value("${wechat.subscribe.template-id.remind:}")
    private String remindTemplateId;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // 云环境 Redis 可能不可用：用内存兜底缓存 token，避免每次都打微信接口
    private volatile String memoryAccessToken = null;
    private volatile long memoryAccessTokenExpireAtMs = 0L;

    /**
     * 获取 access_token（优先从 Redis 读取）
     */
    public String getAccessToken() {
        // 1) 内存缓存（优先，避免 Redis 不可用时频繁报错）
        long now = System.currentTimeMillis();
        if (memoryAccessToken != null && !memoryAccessToken.isEmpty() && now < memoryAccessTokenExpireAtMs) {
            return memoryAccessToken;
        }

        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
                if (cached != null && !cached.isEmpty()) {
                    return cached;
                }
            } catch (Exception e) {
                log.warn("从 Redis 读取 access_token 失败", e);
            }
        }

        String token = fetchAccessTokenFromWechat();
        if (token != null) {
            // 2) 内存缓存
            memoryAccessToken = token;
            memoryAccessTokenExpireAtMs = now + (TOKEN_EXPIRE_SECONDS * 1000L);
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(REDIS_KEY_ACCESS_TOKEN, token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("缓存 access_token 到 Redis 失败", e);
                }
            }
        }
        return token;
    }

    private String fetchAccessTokenFromWechat() {
        try {
            String urlStr = String.format(TOKEN_URL, appid, secret);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            applyWechatHttpsRelaxIfNeeded(conn);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = JSON.parseObject(sb.toString());
                if (json.containsKey("access_token")) {
                    return json.getString("access_token");
                }
                log.error("获取 access_token 失败: {}", json.getString("errmsg"));
            }
        } catch (Exception e) {
            log.error("请求微信 token 接口失败", e);
        }
        return null;
    }

    /**
     * 发送喝水提醒订阅消息（一次性模板）
     * 模板字段：
     * - thing1: 温馨提示（最多 20 字）
     * - thing5: 目标饮水量（最多 20 字）
     * - time6: 饮水时间（格式：HH:mm）
     * - （原 phrase4 距离上次饮水字段在部分模板中为受限枚举，容易触发 47003，这里不再填写）
     *
     * @param openid     用户 openid
     * @param thing1     温馨提示（如：饮水时间到啦,请您记得适量饮水。）
     * @param thing5     目标饮水量（如：1000ml）
     * @param time6      饮水时间（格式：HH:mm，如：14:30）
     * @param phrase4    距离上次饮水（如：2小时，当前实现中不再填入模板，仅用于本地文案）
     */
    public boolean sendRemindMessage(String openid, String thing1, String thing5, String time6, String phrase4) {
        if (remindTemplateId == null || remindTemplateId.trim().isEmpty()) {
            log.warn("未配置订阅消息模板 ID，无法发送提醒");
            return false;
        }

        String token = getAccessToken();
        if (token == null) {
            log.error("获取 access_token 失败，无法发送订阅消息");
            return false;
        }

        // thing 类型最多 20 字符，phrase 类型最多 5 字符
        String t1 = truncate(thing1, 20);
        String t5 = truncate(thing5, 20);
        String p4 = truncate(phrase4, 5);

        Map<String, Object> data = new HashMap<>();
        Map<String, String> thing1Map = new HashMap<>();
        thing1Map.put("value", t1);
        data.put("thing1", thing1Map);
        
        Map<String, String> thing5Map = new HashMap<>();
        thing5Map.put("value", t5);
        data.put("thing5", thing5Map);
        
        Map<String, String> time6Map = new HashMap<>();
        time6Map.put("value", time6);
        data.put("time6", time6Map);

        // 若模板中仍保留 phrase4 字段，则写入固定短语（如「刚刚」「有一会了」「已经好久啦」「首次」）
        Map<String, String> phrase4Map = new HashMap<>();
        phrase4Map.put("value", p4);
        data.put("phrase4", phrase4Map);

        Map<String, Object> body = new HashMap<>();
        body.put("touser", openid);
        body.put("template_id", remindTemplateId.trim());
        body.put("page", "pages/home/home");
        body.put("data", data);

        return doSend(token, body);
    }

    private boolean doSend(String accessToken, Map<String, Object> body) {
        try {
            String urlStr = String.format(SEND_URL, accessToken);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            applyWechatHttpsRelaxIfNeeded(conn);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(JSON.toJSONBytes(body));
            }

            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject json = JSON.parseObject(sb.toString());
            int errcode = json.getIntValue("errcode");
            if (errcode == 0) {
                log.info("订阅消息发送成功");
                return true;
            }
            log.error("订阅消息发送失败 errcode={} errmsg={}", errcode, json.getString("errmsg"));
            return false;
        } catch (Exception e) {
            log.error("发送订阅消息异常", e);
            return false;
        }
    }

    /**
     * 云托管环境下部分基础镜像可能出现证书链不完整导致 PKIX 异常；
     * 这里对「微信官方域名 api.weixin.qq.com」的 HTTPS 调用单独放宽校验。
     */
    private void applyWechatHttpsRelaxIfNeeded(HttpURLConnection conn) throws Exception {
        if (!(conn instanceof HttpsURLConnection)) return;
        HttpsURLConnection https = (HttpsURLConnection) conn;
        https.setSSLSocketFactory(createTrustAllSslSocketFactory());
        https.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return "api.weixin.qq.com".equalsIgnoreCase(hostname);
            }
        });
    }

    private javax.net.ssl.SSLSocketFactory createTrustAllSslSocketFactory() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc.getSocketFactory();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }

    public boolean isTemplateConfigured() {
        return remindTemplateId != null && !remindTemplateId.trim().isEmpty();
    }
}

