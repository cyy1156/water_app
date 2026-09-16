package com.waterapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.waterapp.entity.Goods;
import com.waterapp.mapper.GoodsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品相关业务
 */
@Slf4j
@Service
public class GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 获取上架商品列表，按 sort_order 正序、id 倒序
     */
    public List<Goods> getGoodsList() {
        return goodsMapper.selectList(
                new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getStatus, 1)
                        .orderByAsc(Goods::getSortOrder)
                        .orderByDesc(Goods::getId)
        );
    }

    /**
     * 获取自我奖励类商品列表（商城用）
     */
    public List<Goods> getSelfRewardGoodsList() {
        return goodsMapper.selectList(
                new LambdaQueryWrapper<Goods>()
                        .eq(Goods::getStatus, 1)
                        .and(w -> w.eq(Goods::getType, "SELF_REWARD").or().isNull(Goods::getType))
                        .orderByAsc(Goods::getSortOrder)
                        .orderByDesc(Goods::getId)
        );
    }

    /**
     * 获取单个商品详情（仅允许上架商品）
     */
    public Goods getGoodsDetail(Long goodsId) {
        if (goodsId == null) {
            return null;
        }
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() == 0) {
            return null;
        }
        return goods;
    }
}


