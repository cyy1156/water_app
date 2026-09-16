package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waterapp.common.exception.BusinessException;
import com.waterapp.entity.PointsRecord;
import com.waterapp.entity.User;
import com.waterapp.mapper.PointsRecordMapper;
import com.waterapp.mapper.UserMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分相关业务
 */
@Slf4j
@Service
public class PointsService {

    /** 每100ml获得的积分 */
    private static final int POINTS_PER_100ML = 1;
    /** 每日积分上限 */
    private static final int MAX_POINTS_PER_DAY = 50;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 喝水打卡获得积分
     *
     * @param userId          用户ID
     * @param checkinRecordId 打卡记录ID
     * @param waterMl         本次喝水量（ml）
     * @return 本次实际获得的积分（可能因为达到上限而为0）
     */
    @Transactional
    public int addPointsForCheckin(Long userId, Long checkinRecordId, Integer waterMl) {
        if (userId == null || waterMl == null || waterMl <= 0) {
            return 0;
        }

        // 计算理论可获得积分
        int points = (waterMl / 100) * POINTS_PER_100ML;
        if (points <= 0) {
            return 0;
        }

        LocalDate today = LocalDate.now();

        // 查询当天已通过打卡获得的积分
        Integer todayTotal = pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getUserId, userId)
                        .eq(PointsRecord::getType, "CHECKIN")
                        .ge(PointsRecord::getCreateTime, today.atStartOfDay())
                        .lt(PointsRecord::getCreateTime, today.plusDays(1).atStartOfDay())
        ).stream().mapToInt(pr -> pr.getPoints() == null ? 0 : pr.getPoints()).sum();

        int remaining = MAX_POINTS_PER_DAY - todayTotal;
        if (remaining <= 0) {
            log.info("用户今日打卡积分已达上限 userId={}", userId);
            return 0;
        }

        // 本次最多只能拿到 remaining
        int actualPoints = Math.min(points, remaining);
        if (actualPoints <= 0) {
            return 0;
        }

        // 更新用户总积分
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        int originTotal = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        user.setTotalPoints(originTotal + actualPoints);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 记录积分流水
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setPoints(actualPoints);
        record.setType("CHECKIN");
        record.setRelatedId(checkinRecordId);
        record.setDescription("喝水打卡获得积分");
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);

        log.info("用户打卡获得积分 userId={}, waterMl={}, points={}", userId, waterMl, actualPoints);
        return actualPoints;
    }

    /**
     * 扣减积分（用于兑换等场景）
     */
    @Transactional
    public void deductPoints(Long userId, int points, Long relatedId, String description) {
        if (userId == null || points <= 0) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        int originTotal = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        if (originTotal < points) {
            throw new BusinessException("积分不足");
        }

        user.setTotalPoints(originTotal - points);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setPoints(-points);
        record.setType("EXCHANGE");
        record.setRelatedId(relatedId);
        record.setDescription(description != null ? description : "积分兑换");
        record.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(record);
    }

    /**
     * 获取用户积分记录列表（按时间倒序）
     */
    public List<PointsRecord> getPointsList(Long userId) {
        return pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getUserId, userId)
                        .orderByDesc(PointsRecord::getCreateTime)
        );
    }

    /**
     * 获取用户积分统计信息
     */
    public PointsStatisticsVO getStatistics(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<PointsRecord> records = getPointsList(userId);
        int earned = records.stream()
                .filter(r -> r.getPoints() != null && r.getPoints() > 0)
                .mapToInt(PointsRecord::getPoints)
                .sum();
        int spent = records.stream()
                .filter(r -> r.getPoints() != null && r.getPoints() < 0)
                .mapToInt(PointsRecord::getPoints)
                .sum();

        PointsStatisticsVO vo = new PointsStatisticsVO();
        vo.setTotalPoints(user.getTotalPoints() == null ? 0 : user.getTotalPoints());
        vo.setTotalEarned(earned);
        vo.setTotalSpent(spent);
        return vo;
    }

    /**
     * 积分统计 VO
     */
    @Data
    public static class PointsStatisticsVO {
        /** 当前总积分 */
        private Integer totalPoints;
        /** 累计获得积分 */
        private Integer totalEarned;
        /** 累计消耗积分（负数） */
        private Integer totalSpent;
    }
}


