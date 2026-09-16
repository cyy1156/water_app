package com.waterapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.waterapp.entity.UserReminderSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户提醒设置 Mapper（M4）
 */
@Mapper
public interface UserReminderSettingMapper extends BaseMapper<UserReminderSetting> {
}

