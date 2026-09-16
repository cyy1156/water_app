package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.entity.PointsRecord;
import com.waterapp.service.PointsService;
import com.waterapp.service.PointsService.PointsStatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 积分相关接口
 */
@Slf4j
@RestController
@RequestMapping("/points")
public class PointsController {

    @Autowired
    private PointsService pointsService;

    /**
     * 获取积分记录列表
     */
    @GetMapping("/list")
    public Result<List<PointsRecord>> getList(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<PointsRecord> list = pointsService.getPointsList(userId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取积分记录列表失败", e);
            return Result.error("获取积分记录列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取积分统计信息
     */
    @GetMapping("/statistics")
    public Result<PointsStatisticsVO> getStatistics(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            PointsStatisticsVO vo = pointsService.getStatistics(userId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取积分统计信息失败", e);
            return Result.error("获取积分统计信息失败：" + e.getMessage());
        }
    }
}


