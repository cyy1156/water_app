package com.waterapp.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    /**
     * 微信登录code
     */
    @NotBlank(message = "code不能为空")
    private String code;
}

