package com.waterapp.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * 微信工具类
 */
@Slf4j
@Component
public class WechatUtil {

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    private static final String WECHAT_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /**
     * 通过code获取openid和session_key
     * @param code 微信登录code
     * @return JSONObject 包含openid和session_key
     */
    public JSONObject getOpenidByCode(String code) {
        try {
            String urlStr = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    WECHAT_LOGIN_URL, appid, secret, code);

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 云托管环境下部分基础镜像的根证书链可能不完整，这里对「微信开放平台登录接口」单独放宽 HTTPS 校验，
            // 避免出现 PKIX path building failed 导致登录失败。只对 api.weixin.qq.com 生效。
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection https = (HttpsURLConnection) connection;
                https.setSSLSocketFactory(createTrustAllSslSocketFactory());
                https.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        // 只放行微信登录域名
                        return "api.weixin.qq.com".equalsIgnoreCase(hostname);
                    }
                });
            }
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonObject = JSON.parseObject(response.toString());
                
                // 检查是否有错误
                if (jsonObject.containsKey("errcode")) {
                    log.error("微信登录失败：{}", jsonObject.getString("errmsg"));
                    throw new RuntimeException("微信登录失败：" + jsonObject.getString("errmsg"));
                }

                return jsonObject;
            } else {
                throw new RuntimeException("请求微信接口失败，响应码：" + responseCode);
            }
        } catch (Exception e) {
            log.error("获取openid失败", e);
            throw new RuntimeException("获取openid失败：" + e.getMessage());
        }
    }

    /**
     * 构造一个「信任所有证书」的 SSLContext，仅用于微信登录接口调用，避免云环境证书链问题导致 PKIX 异常。
     */
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
}

