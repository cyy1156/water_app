package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.service.PersonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 粥粥人设与话术接口（未完成项清单 - 任务1）
 * GET /ai/persona：返回粥粥人设与当前话术
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiPersonaController {

    @Autowired
    private PersonaService personaService;

    /**
     * 获取粥粥人设与当前话术（或占位文案）
     * GET /ai/persona
     */
    @GetMapping("/persona")
    public Result<PersonaService.PersonaVO> getPersona(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            PersonaService.PersonaVO vo = personaService.getPersona(userId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取人设失败", e);
            return Result.error("获取人设失败：" + e.getMessage());
        }
    }
}
