package com.waterapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 喝水健康陪伴小程序 - 后端应用启动类
 */
@SpringBootApplication
@MapperScan("com.waterapp.mapper")
@EnableScheduling
public class WaterAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaterAppApplication.class, args);
        System.out.println("=================================");
        System.out.println("喝水健康陪伴小程序后端启动成功！");
        System.out.println("=================================");
    }
}

