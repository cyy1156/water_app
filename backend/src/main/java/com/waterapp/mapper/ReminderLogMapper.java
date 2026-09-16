package com.waterapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waterapp.entity.ReminderLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提醒日志Mapper接口
 */
@Mapper
public interface ReminderLogMapper extends BaseMapper<ReminderLog> {
}

