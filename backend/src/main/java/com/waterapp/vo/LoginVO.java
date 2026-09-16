package com.waterapp.vo;

import lombok.Data;

/**
 * 登录响应VO
 */
@Data
public class LoginVO {
    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserVO userInfo;
}

