package com.waterapp.controller;

import com.waterapp.common.Result;
import com.waterapp.entity.ExchangeRecord;
import com.waterapp.entity.Goods;
import com.waterapp.service.GoodsService;
import com.waterapp.service.StoreService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 大升级：金币商城接口（对前端暴露为 /store 前缀）
 *
 * M2阶段：完善金币购买闭环
 * - 使用金币（coin）购买商品
 * - 购买时扣减coin并同步到totalPoints（保持兼容）
 * - 购买记录写入金币流水（type=PURCHASE）
 */
@Slf4j
@RestController
@RequestMapping("/store")
public class StoreController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private StoreService storeService;

    /**
     * 获取金币商城商品列表（仅自我奖励类）
     * GET /store/goods/list
     */
    @GetMapping("/goods/list")
    public Result<List<StoreGoodsVO>> listGoods() {
        try {
            List<Goods> list = goodsService.getSelfRewardGoodsList();
            List<StoreGoodsVO> result = new ArrayList<>();
            for (Goods g : list) {
                StoreGoodsVO vo = new StoreGoodsVO();
                vo.setId(g.getId());
                vo.setName(g.getName());
                vo.setDescription(g.getDescription());
                vo.setImageUrl(g.getImageUrl());
                vo.setPriceCoin(g.getPointsRequired());
                vo.setStock(g.getStock());
                result.add(vo);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取金币商城商品列表失败", e);
            return Result.error("获取商品列表失败：" + e.getMessage());
        }
    }

    /**
     * 使用金币购买商品（M2阶段：完善金币购买闭环）
     * POST /store/purchase
     * body: { "goodsId": 1, "address": "可选收货地址" }
     */
    @PostMapping("/purchase")
    public Result<PurchaseResultVO> purchase(HttpServletRequest request,
                                             @RequestBody PurchaseBody body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未授权，请先登录");
            }
            Long goodsId = body.getGoodsId();
            String address = body.getAddress();
            LocalDateTime remindTime = null;
            if (body.getRemindTime() != null && !body.getRemindTime().trim().isEmpty()) {
                try {
                    remindTime = LocalDateTime.parse(body.getRemindTime().trim(),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception ignored) {}
            }

            StoreService.PurchaseResult result = storeService.purchaseGoods(userId, goodsId, address, remindTime);

            PurchaseResultVO vo = new PurchaseResultVO();
            vo.setRecordId(result.getRecord().getId());
            vo.setGoodsId(result.getRecord().getGoodsId());
            vo.setGoodsName(result.getRecord().getGoodsName());
            vo.setCoinUsed(result.getCoinUsed());
            vo.setStatus(result.getRecord().getStatus());
            vo.setRemainingCoin(result.getRemainingCoin());
            vo.setReminderId(result.getReminderId());
            return Result.success(vo);
        } catch (Exception e) {
            log.error("金币商城购买失败", e);
            return Result.error("购买失败：" + e.getMessage());
        }
    }

    /**
     * 用户确认「完成」自我奖励，写入成就
     * POST /store/confirm-done  body: { "recordId": 123 }
     */
    @PostMapping("/confirm-done")
    public Result<Void> confirmDone(HttpServletRequest request, @RequestBody ConfirmDoneBody body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) return Result.error(401, "未授权，请先登录");
            if (body == null || body.getRecordId() == null) {
                return Result.badRequest("缺少 recordId");
            }
            storeService.confirmRedeemDone(userId, body.getRecordId());
            return Result.success();
        } catch (Exception e) {
            log.error("确认完成失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 待确认完成的兑换记录（已兑换但尚未点「完成」的）
     * GET /store/pending-done
     */
    @GetMapping("/pending-done")
    public Result<List<PendingDoneVO>> pendingDone(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) return Result.error(401, "未授权，请先登录");
            List<ExchangeRecord> list = storeService.listPendingRedeem(userId);
            List<PendingDoneVO> result = new ArrayList<>();
            for (ExchangeRecord r : list) {
                PendingDoneVO vo = new PendingDoneVO();
                vo.setRecordId(r.getId());
                vo.setGoodsName(r.getGoodsName());
                vo.setCreateTime(r.getCreateTime());
                result.add(vo);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取待确认列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Data
    public static class ConfirmDoneBody {
        private Long recordId;
    }

    @Data
    public static class PendingDoneVO {
        private Long recordId;
        private String goodsName;
        private LocalDateTime createTime;
    }

    @Data
    public static class PurchaseBody {
        private Long goodsId;
        private String address; // 可选
        private String remindTime; // 可选，ISO 日期时间，如 2026-02-25T14:00:00
    }

    @Data
    public static class StoreGoodsVO {
        private Long id;
        private String name;
        private String description;
        private String imageUrl;
        private Integer priceCoin;
        private Integer stock;
    }

    @Data
    public static class PurchaseResultVO {
        private Long recordId;
        private Long goodsId;
        private String goodsName;
        private Integer coinUsed;
        private Integer status;
        private Integer remainingCoin;
        private Long reminderId;
    }
}


