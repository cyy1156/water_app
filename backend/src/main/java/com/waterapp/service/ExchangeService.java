package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.waterapp.common.exception.BusinessException;
import com.waterapp.entity.ExchangeRecord;
import com.waterapp.entity.Goods;
import com.waterapp.entity.User;
import com.waterapp.mapper.ExchangeRecordMapper;
import com.waterapp.mapper.GoodsMapper;
import com.waterapp.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分兑换相关业务
 */
@Slf4j
@Service
public class ExchangeService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private ExchangeRecordMapper exchangeRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PointsService pointsService;

    /**
     * 兑换商品：库存检查（乐观锁）+ 积分扣除 + 创建兑换记录
     */
    @Transactional
    public ExchangeRecord exchangeGoods(Long userId, Long goodsId, String address) {
        if (userId == null || goodsId == null) {
            throw new BusinessException("参数错误");
        }

        // 查询商品
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() == 0) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (goods.getStock() == null || goods.getStock() <= 0) {
            throw new BusinessException("商品库存不足");
        }

        // 查询用户，检查积分
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        int totalPoints = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        int needPoints = goods.getPointsRequired() == null ? 0 : goods.getPointsRequired();
        if (needPoints <= 0) {
            throw new BusinessException("商品积分配置异常");
        }
        if (totalPoints < needPoints) {
            throw new BusinessException("积分不足");
        }

        // 库存扣减（乐观锁：where stock > 0）
        LambdaUpdateWrapper<Goods> updateWrapper = new LambdaUpdateWrapper<Goods>()
                .eq(Goods::getId, goodsId)
                .gt(Goods::getStock, 0)
                .set(Goods::getStock, goods.getStock() - 1);
        int rows = goodsMapper.update(null, updateWrapper);
        if (rows == 0) {
            throw new BusinessException("库存不足，请稍后再试");
        }

        // 扣除积分（写入积分流水）
        pointsService.deductPoints(userId, needPoints, null, "兑换商品：" + goods.getName());

        // 创建兑换记录
        LocalDateTime now = LocalDateTime.now();
        ExchangeRecord record = new ExchangeRecord();
        record.setUserId(userId);
        record.setGoodsId(goodsId);
        record.setGoodsName(goods.getName());
        record.setPointsUsed(needPoints);
        record.setStatus(0); // 0-待发货
        record.setAddress(address);
        record.setCreateTime(now);
        record.setUpdateTime(now);

        exchangeRecordMapper.insert(record);

        log.info("用户兑换商品成功 userId={}, goodsId={}, points={}", userId, goodsId, needPoints);
        return record;
    }

    /**
     * 获取当前用户的兑换记录列表（按时间倒序）
     */
    public List<ExchangeRecord> getExchangeList(Long userId) {
        return exchangeRecordMapper.selectList(
                new LambdaQueryWrapper<ExchangeRecord>()
                        .eq(ExchangeRecord::getUserId, userId)
                        .orderByDesc(ExchangeRecord::getCreateTime)
        );
    }
}


