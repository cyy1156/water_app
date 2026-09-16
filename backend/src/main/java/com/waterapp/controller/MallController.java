package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.entity.ExchangeRecord;
import com.waterapp.entity.Goods;
import com.waterapp.service.ExchangeService;
import com.waterapp.service.GoodsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 商城相关接口
 */
@Slf4j
@RestController
@RequestMapping("/mall")
public class MallController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private ExchangeService exchangeService;

    /**
     * 获取商品列表
     * GET /mall/goods/list
     */
    @GetMapping("/goods/list")
    public Result<List<Goods>> getGoodsList() {
        try {
            List<Goods> list = goodsService.getGoodsList();
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            return Result.error("获取商品列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取商品详情
     * GET /mall/goods/detail?goodsId=1
     */
    @GetMapping("/goods/detail")
    public Result<Goods> getGoodsDetail(@RequestParam("goodsId") Long goodsId) {
        try {
            Goods goods = goodsService.getGoodsDetail(goodsId);
            if (goods == null) {
                return Result.badRequest("商品不存在或已下架");
            }
            return Result.success(goods);
        } catch (Exception e) {
            log.error("获取商品详情失败", e);
            return Result.error("获取商品详情失败：" + e.getMessage());
        }
    }

    /**
     * 兑换商品
     * POST /mall/exchange
     * body: { "goodsId": 1, "address": "xxx" }
     */
    @PostMapping("/exchange")
    public Result<ExchangeRecord> exchange(HttpServletRequest request,
                                           @RequestBody ExchangeRequest body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            Long goodsId = body.getGoodsId();
            String address = body.getAddress();
            ExchangeRecord record = exchangeService.exchangeGoods(userId, goodsId, address);
            return Result.success(record);
        } catch (Exception e) {
            log.error("兑换商品失败", e);
            return Result.error("兑换商品失败：" + e.getMessage());
        }
    }

    /**
     * 获取兑换记录列表
     * GET /mall/exchange/list
     */
    @GetMapping("/exchange/list")
    public Result<List<ExchangeRecord>> getExchangeList(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            List<ExchangeRecord> list = exchangeService.getExchangeList(userId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取兑换记录列表失败", e);
            return Result.error("获取兑换记录列表失败：" + e.getMessage());
        }
    }

    /**
     * 兑换请求体
     */
    public static class ExchangeRequest {
        private Long goodsId;
        private String address;

        public Long getGoodsId() {
            return goodsId;
        }

        public void setGoodsId(Long goodsId) {
            this.goodsId = goodsId;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}


