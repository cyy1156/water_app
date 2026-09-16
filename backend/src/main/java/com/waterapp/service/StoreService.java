package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.waterapp.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waterapp.entity.*;
import com.waterapp.mapper.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 金币商城服务（M2阶段：完善金币购买闭环）
 * 
 * 说明：
 * - 使用金币（coin）购买商品
 * - 购买时扣减coin并同步到totalPoints（保持兼容）
 * - 购买记录写入金币流水（type=PURCHASE）
 */
@Slf4j
@Service
public class StoreService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private ExchangeRecordMapper exchangeRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    @Autowired
    private RewardReminderMapper rewardReminderMapper;

    @Autowired
    private AchievementMapper achievementMapper;

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 使用金币兑换自我奖励
     * @param remindTime 提醒时间（可选），ISO 格式字符串
     */
    @Transactional
    public PurchaseResult purchaseGoods(Long userId, Long goodsId, String address, LocalDateTime remindTime) {
        if (userId == null || goodsId == null) {
            throw new BusinessException("参数错误");
        }

        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() == 0) {
            throw new BusinessException("商品不存在或已下架");
        }
        // 自我奖励类（SELF_REWARD 或 type 为空）不校验库存；实物类需 stock > 0
        boolean selfReward = "SELF_REWARD".equals(goods.getType()) || goods.getType() == null;
        if (!selfReward && (goods.getStock() == null || goods.getStock() <= 0)) {
            throw new BusinessException("商品库存不足");
        }

        int priceCoin = goods.getPointsRequired() == null ? 0 : goods.getPointsRequired();
        if (priceCoin <= 0) {
            throw new BusinessException("商品价格配置异常");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        int currentCoin = user.getCoin() != null
                ? user.getCoin()
                : (user.getTotalPoints() != null ? user.getTotalPoints() : 0);
        if (currentCoin < priceCoin) {
            throw new BusinessException("金币不足");
        }

        if (!selfReward && goods.getStock() != null && goods.getStock() > 0) {
            LambdaUpdateWrapper<Goods> updateWrapper = new LambdaUpdateWrapper<Goods>()
                    .eq(Goods::getId, goodsId)
                    .gt(Goods::getStock, 0)
                    .set(Goods::getStock, goods.getStock() - 1);
            if (goodsMapper.update(null, updateWrapper) == 0) {
                throw new BusinessException("库存不足，请稍后再试");
            }
        }

        int newCoin = currentCoin - priceCoin;
        user.setCoin(newCoin);
        user.setTotalPoints(newCoin);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        PointsRecord coinRecord = new PointsRecord();
        coinRecord.setUserId(userId);
        coinRecord.setPoints(-priceCoin);
        coinRecord.setType("PURCHASE");
        coinRecord.setRelatedId(goodsId);
        coinRecord.setDescription("兑换自我奖励：" + goods.getName());
        coinRecord.setCreateTime(LocalDateTime.now());
        pointsRecordMapper.insert(coinRecord);

        LocalDateTime now = LocalDateTime.now();
        ExchangeRecord record = new ExchangeRecord();
        record.setUserId(userId);
        record.setGoodsId(goodsId);
        record.setGoodsName(goods.getName());
        record.setPointsUsed(priceCoin);
        record.setStatus(0);
        record.setAddress(address);
        record.setExecuted(0);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        exchangeRecordMapper.insert(record);

        Long reminderId = null;
        RewardReminder rr = new RewardReminder();
        rr.setUserId(userId);
        rr.setExchangeRecordId(record.getId());
        rr.setContent("奖励自己：" + goods.getName());
        rr.setRemindTime(remindTime);
        rr.setStatus(0);
        rr.setCreateTime(now);
        rewardReminderMapper.insert(rr);
        reminderId = rr.getId();

        record.setReminderId(reminderId);
        exchangeRecordMapper.updateById(record);

        // 不再在此处写入成就，等用户在小程序里点击「完成」后再写入
        log.info("兑换自我奖励成功 userId={}, goodsId={}, coinUsed={}, remainingCoin={}, reminderId={}, recordId={}",
                userId, goodsId, priceCoin, newCoin, reminderId, record.getId());

        PurchaseResult result = new PurchaseResult();
        result.setRecord(record);
        result.setRemainingCoin(newCoin);
        result.setCoinUsed(priceCoin);
        result.setReminderId(reminderId);
        return result;
    }

    /**
     * 获取当前用户的购买记录列表（按时间倒序）
     */
    public List<ExchangeRecord> getPurchaseList(Long userId) {
        return exchangeRecordMapper.selectList(
                new LambdaQueryWrapper<ExchangeRecord>()
                        .eq(ExchangeRecord::getUserId, userId)
                        .orderByDesc(ExchangeRecord::getCreateTime)
        );
    }

    /**
     * 用户确认「完成」后写入成就（仅限本用户的兑换记录且尚未写入成就的）
     */
    @Transactional
    public void confirmRedeemDone(Long userId, Long exchangeRecordId) {
        if (userId == null || exchangeRecordId == null) {
            throw new BusinessException("参数错误");
        }
        ExchangeRecord record = exchangeRecordMapper.selectOne(
                new LambdaQueryWrapper<ExchangeRecord>()
                        .eq(ExchangeRecord::getId, exchangeRecordId)
                        .eq(ExchangeRecord::getUserId, userId)
        );
        if (record == null) {
            throw new BusinessException("兑换记录不存在");
        }
        long already = achievementMapper.selectCount(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getExchangeRecordId, exchangeRecordId)
                        .eq(Achievement::getType, "REDEEMED")
        );
        if (already > 0) {
            return; // 已确认过，幂等
        }
        Achievement achievement = new Achievement();
        achievement.setUserId(userId);
        achievement.setType("REDEEMED");
        Map<String, Object> config = new HashMap<>();
        config.put("goodsName", record.getGoodsName() != null ? record.getGoodsName() : "自我奖励");
        try {
            achievement.setConfigJson(JSON.writeValueAsString(config));
        } catch (Exception e) {
            achievement.setConfigJson("{\"goodsName\":\"" + (record.getGoodsName() != null ? record.getGoodsName().replace("\"", "\\\"") : "自我奖励") + "\"}");
        }
        achievement.setCompletedAt(LocalDateTime.now());
        achievement.setExchangeRecordId(record.getId());
        achievementMapper.insert(achievement);
        log.info("用户确认完成兑换 userId={}, recordId={}, achievementId={}", userId, exchangeRecordId, achievement.getId());
    }

    /**
     * 获取待确认完成的兑换记录（已兑换但尚未点「完成」的）
     */
    public List<ExchangeRecord> listPendingRedeem(Long userId) {
        if (userId == null) return Collections.emptyList();
        List<ExchangeRecord> all = exchangeRecordMapper.selectList(
                new LambdaQueryWrapper<ExchangeRecord>()
                        .eq(ExchangeRecord::getUserId, userId)
                        .orderByDesc(ExchangeRecord::getCreateTime)
        );
        List<Long> alreadyIds = achievementMapper.selectList(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getUserId, userId)
                        .eq(Achievement::getType, "REDEEMED")
                        .isNotNull(Achievement::getExchangeRecordId)
        ).stream().map(Achievement::getExchangeRecordId).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> set = new HashSet<>(alreadyIds);
        return all.stream().filter(r -> !set.contains(r.getId())).collect(Collectors.toList());
    }

    @Data
    public static class PurchaseResult {
        private ExchangeRecord record;
        private Integer remainingCoin;
        private Integer coinUsed;
        private Long reminderId;
    }
}

