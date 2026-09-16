package com.waterapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waterapp.entity.Goods;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品Mapper接口
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
}

