package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waterapp.common.exception.BusinessException;
import com.waterapp.dto.LoginRequest;
import com.waterapp.entity.User;
import com.waterapp.mapper.UserMapper;
import com.waterapp.util.JwtUtil;
import com.waterapp.util.WechatUtil;
import com.waterapp.vo.LoginVO;
import com.waterapp.vo.UserVO;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 用户服务类
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WechatUtil wechatUtil;

    @Value("${app.avatar.upload-dir:uploads/avatar}")
    private String avatarUploadDir;

    /**
     * 上传用户头像，返回可访问的 URL（用于首次登录 chooseAvatar）
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;
        try {
            Path dir = Paths.get(avatarUploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            return "/uploads/avatar/" + filename;
        } catch (IOException e) {
            log.error("保存头像失败", e);
            throw new BusinessException("头像上传失败，请稍后重试");
        }
    }

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应（包含token和用户信息）
     */
    @Transactional
    public LoginVO login(LoginRequest request) {
        // 1. 通过code获取openid
        JSONObject wechatResult = wechatUtil.getOpenidByCode(request.getCode());
        String openid = wechatResult.getString("openid");
        String sessionKey = wechatResult.getString("session_key");
        String unionid = wechatResult.getString("unionid");

        if (openid == null) {
            throw new BusinessException("获取openid失败");
        }

        // 2. 查询用户是否存在
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid)
        );

        // 3. 如果用户不存在，创建新用户
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setTargetWater(2000); // 默认目标水量
            user.setTotalPoints(0);
            // 大升级：成长系统默认值
            user.setLevel(1);
            user.setExp(0);
            user.setCoin(0);
            user.setGrowthStage("幼苗");
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
            log.info("新用户注册：openid={}", openid);
        }

        // 4. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), openid);

        // 5. 构建返回对象
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        loginVO.setUserInfo(userVO);

        return loginVO;
    }

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param userVO 用户信息
     */
    @Transactional
    public void updateUserInfo(Long userId, UserVO userVO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 更新允许修改的字段
        if (userVO.getNickname() != null) {
            user.setNickname(userVO.getNickname());
        }
        if (userVO.getAvatarUrl() != null) {
            user.setAvatarUrl(userVO.getAvatarUrl());
        }
        if (userVO.getTargetWater() != null) {
            user.setTargetWater(userVO.getTargetWater());
        }
        if (userVO.getCoverImage() != null) {
            user.setCoverImage(userVO.getCoverImage());
        }
        if (userVO.getCoverText() != null) {
            user.setCoverText(userVO.getCoverText());
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 获取用户积分
     * @param userId 用户ID
     * @return 总积分
     */
    public Integer getUserPoints(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user.getTotalPoints() != null ? user.getTotalPoints() : 0;
    }

    /**
     * M5：首次登录个性化方案
     * 根据头像、昵称、性别推荐方案，并更新用户信息
     */
    @Transactional
    public FirstLoginVO firstLogin(Long userId, String avatarUrl, String nickName, Integer gender) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        LocalDateTime now = LocalDateTime.now();

        // 更新用户信息
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            user.setAvatarUrl(avatarUrl.trim());
        }
        if (nickName != null && !nickName.trim().isEmpty()) {
            user.setNickname(nickName.trim());
        }
        if (gender != null) {
            user.setGender(gender);
        }
        user.setFirstLoginTime(now);
        user.setUpdateTime(now);
        userMapper.updateById(user);

        // 根据性别推荐
        int recommendedTarget = 2000;
        String recommendedStartTime = "09:00";
        String recommendedEndTime = "21:00";
        int recommendedInterval = 90;
        String recommendedTheme = "green";

        if (gender != null) {
            if (gender == 1) { // 男性
                recommendedTarget = 2500;
                recommendedStartTime = "08:00";
                recommendedEndTime = "22:00";
            } else if (gender == 2) { // 女性
                recommendedTarget = 2000;
                recommendedStartTime = "09:00";
                recommendedEndTime = "21:00";
            }
        }

        // 昵称分析（可选）
        String recommendedIntensity = "SOFT";
        if (nickName != null && (nickName.contains("小") || nickName.contains("宝"))) {
            recommendedIntensity = "SOFT";
        }

        user.setPersonalizedTarget(recommendedTarget);
        userMapper.updateById(user);

        FirstLoginVO vo = new FirstLoginVO();
        vo.setRecommendedTarget(recommendedTarget);
        vo.setRecommendedStartTime(recommendedStartTime);
        vo.setRecommendedEndTime(recommendedEndTime);
        vo.setRecommendedInterval(recommendedInterval);
        vo.setRecommendedTheme(recommendedTheme);
        vo.setRecommendedIntensity(recommendedIntensity);
        vo.setWelcomeMessage("欢迎" + (nickName != null ? nickName : "小可爱") + "，粥粥为你定制了专属方案～");
        return vo;
    }

    /**
     * M5：接受个性化方案（保存目标、提醒设置）
     */
    @Transactional
    public void acceptFirstLoginPlan(Long userId, int targetWater, String startTime, String endTime, int intervalMin) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setTargetWater(targetWater);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        // 提醒设置由 ReminderService 保存，前端会调用 /ai/reminder/settings
    }

    /**
     * M5：更新每日喝水目标
     */
    @Transactional
    public void updateTargetWater(Long userId, int targetWater) {
        if (targetWater < 500 || targetWater > 5000) {
            throw new BusinessException("目标水量需在 500-5000 ml 之间");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setTargetWater(targetWater);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @lombok.Data
    public static class FirstLoginVO {
        private Integer recommendedTarget;
        private String recommendedStartTime;
        private String recommendedEndTime;
        private Integer recommendedInterval;
        private String recommendedTheme;
        private String recommendedIntensity;
        private String welcomeMessage;
    }
}

