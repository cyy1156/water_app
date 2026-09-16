package com.waterapp.config;

import com.waterapp.interceptor.AuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC配置类
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Value("${app.chat.upload-dir:uploads/chat}")
    private String chatUploadDir;

    @Value("${app.goods.upload-dir:uploads/goods}")
    private String goodsUploadDir;

    @Value("${app.avatar.upload-dir:uploads/avatar}")
    private String avatarUploadDir;

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 聊天图片访问路径
        String chatUploadPath = Paths.get(chatUploadDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/chat/**")
                .addResourceLocations("file:" + chatUploadPath + "/");
        log.info("静态资源映射：/uploads/chat/** -> file:{}/", chatUploadPath);
        
        // 商品图片：优先本地目录，回退到 classpath（jar 内，用于云托管）
        String goodsUploadPath = Paths.get(goodsUploadDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/goods/**")
                .addResourceLocations("file:" + goodsUploadPath + "/", "classpath:/static/uploads/goods/");
        log.info("静态资源映射：/uploads/goods/** -> file:{}/, classpath:/static/uploads/goods/", goodsUploadPath);

        // 用户头像访问路径
        String avatarUploadPath = Paths.get(avatarUploadDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/avatar/**")
                .addResourceLocations("file:" + avatarUploadPath + "/");
        log.info("静态资源映射：/uploads/avatar/** -> file:{}/", avatarUploadPath);
    }

    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",      // 登录接口不需要鉴权
                        "/error",           // 错误页面
                        "/favicon.ico",     // 图标
                        "/uploads/**"       // 静态资源不需要鉴权
                );
    }
}

