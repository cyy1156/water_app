package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.dto.LoginRequest;
import com.waterapp.service.UserService;
import com.waterapp.vo.LoginVO;
import com.waterapp.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 微信登录
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginVO loginVO = userService.login(request);
            return Result.success(loginVO);
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 兼容浏览器直接访问 /user/login 的情况
     * 提示必须使用 POST 方法
     */
    @GetMapping("/login")
    public Result<?> loginGetTip() {
        return Result.badRequest("请使用 POST 方法调用 /user/login，这个地址仅供小程序登录使用");
    }

    /**
     * 获取用户信息
     * @param request HTTP请求（用于获取userId）
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getUserInfo(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            UserVO userVO = userService.getUserInfo(userId);
            return Result.success(userVO);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     * @param request HTTP请求（用于获取userId）
     * @param userVO 用户信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<?> updateUserInfo(HttpServletRequest httpRequest, @RequestBody UserVO userVO) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            userService.updateUserInfo(userId, userVO);
            return Result.success();
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户积分
     * @param request HTTP请求（用于获取userId）
     * @return 用户积分
     */
    @GetMapping("/points")
    public Result<Integer> getUserPoints(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            Integer points = userService.getUserPoints(userId);
            return Result.success(points);
        } catch (Exception e) {
            log.error("获取用户积分失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 上传用户头像（用于首次登录 chooseAvatar）
     * POST /user/avatar/upload, multipart form: file
     */
    @PostMapping("/avatar/upload")
    public Result<Map<String, String>> uploadAvatar(HttpServletRequest request,
                                                    @RequestParam("file") MultipartFile file) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            String url = userService.uploadAvatar(userId, file);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return Result.success(result);
        } catch (Exception e) {
            log.error("上传头像失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * M5：首次登录个性化方案
     * POST /user/first-login
     * body: { "avatarUrl": "...", "nickName": "...", "gender": 1 }
     */
    @PostMapping("/first-login")
    public Result<UserService.FirstLoginVO> firstLogin(HttpServletRequest request,
                                                       @RequestBody FirstLoginBody body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            UserService.FirstLoginVO vo = userService.firstLogin(
                    userId,
                    body != null ? body.getAvatarUrl() : null,
                    body != null ? body.getNickName() : null,
                    body != null ? body.getGender() : null
            );
            return Result.success(vo);
        } catch (Exception e) {
            log.error("首次登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * M5：更新每日喝水目标
     * POST /user/settings/target
     * body: { "targetWater": 2500 }
     */
    @PostMapping("/settings/target")
    public Result<Void> updateTarget(HttpServletRequest request,
                                    @RequestBody TargetBody body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            Integer target = body != null ? body.getTargetWater() : null;
            if (target == null || target < 500 || target > 5000) {
                return Result.badRequest("目标水量需在 500-5000 ml 之间");
            }
            userService.updateTargetWater(userId, target);
            return Result.success();
        } catch (Exception e) {
            log.error("更新目标失败", e);
            return Result.error(e.getMessage());
        }
    }

    @lombok.Data
    public static class FirstLoginBody {
        private String avatarUrl;
        private String nickName;
        private Integer gender;
    }

    @lombok.Data
    public static class TargetBody {
        private Integer targetWater;
    }
}

