package com.waterapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waterapp.entity.CheckinRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打卡记录Mapper接口
 */
@Mapper
public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {
}

