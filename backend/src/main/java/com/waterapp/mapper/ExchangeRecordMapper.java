package com.waterapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waterapp.entity.ExchangeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 兑换记录Mapper接口
 */
@Mapper
public interface ExchangeRecordMapper extends BaseMapper<ExchangeRecord> {
}

