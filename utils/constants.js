// utils/constants.js - 常量定义

/**
 * API接口路径
 */
const API = {
  // 用户相关
  USER_LOGIN: '/user/login',
  USER_INFO: '/user/info',
  USER_UPDATE: '/user/update',
  USER_POINTS: '/user/points',
  
  // 打卡相关
  CHECKIN_TODAY: '/checkin/today',
  CHECKIN_RECORD: '/checkin/record',
  CHECKIN_HISTORY: '/checkin/history',
  CHECKIN_CALENDAR: '/checkin/calendar',

  // 大升级：成长页聚合与结算（新接口优先，不存在则前端回退旧接口）
  HOME_SUMMARY: '/home/summary',
  HOME_CHECKIN: '/home/checkin',

  // 大升级：我的页聚合
  ME_SUMMARY: '/me/summary',

  // 大升级：金币商城（新接口）
  STORE_GOODS_LIST: '/store/goods/list',
  STORE_PURCHASE: '/store/purchase',
  STORE_CONFIRM_DONE: '/store/confirm-done',
  STORE_PENDING_DONE: '/store/pending-done',
  
  // 积分相关
  POINTS_LIST: '/points/list',
  POINTS_STATISTICS: '/points/statistics',
  
  // 商城相关
  MALL_GOODS_LIST: '/mall/goods/list',
  MALL_GOODS_DETAIL: '/mall/goods/detail',
  MALL_EXCHANGE: '/mall/exchange',
  MALL_EXCHANGE_LIST: '/mall/exchange/list',
  
  // AI对话相关
  CHAT_SEND: '/chat/send',
  CHAT_HISTORY: '/chat/history',
  CHAT_UPLOAD: '/chat/upload',

  // 大升级：AI 人设/提醒（微信订阅消息）M4
  AI_PERSONA: '/ai/persona',
  AI_REMINDER_SETTINGS: '/ai/reminder/settings',
  AI_REMINDER_SUBSCRIBE: '/ai/reminder/subscribe',
  AI_REMINDER_TEMPLATE_ID: '/ai/reminder/template-id',
  AI_REMINDER_SEND_NOW: '/ai/reminder/send-now',
  AI_REMINDER_PREVIEW: '/ai/reminder/preview',

  // M5：个性化方案、日历、用户设置
  USER_AVATAR_UPLOAD: '/user/avatar/upload',
  USER_FIRST_LOGIN: '/user/first-login',
  USER_SETTINGS_TARGET: '/user/settings/target',
  CALENDAR_STATS: '/calendar/stats',

  // 成就（商城与成就需求）
  ACHIEVEMENT_LIST: '/achievement/list'
};

/**
 * 存储键名
 */
const STORAGE_KEY = {
  TOKEN: 'token',
  USER_INFO: 'userInfo',
  TARGET_WATER: 'targetWater',
  CHAT_SESSION_ID: 'chatSessionId',  // M3：首页内嵌聊天会话持久化
  LAST_SUBSCRIBE_REQUEST: 'lastSubscribeRequestTime',  // 一次性订阅：上次请求授权时间（节流）
  FIRST_LOGIN_DONE: 'firstLoginDone'  // M5：是否已完成首次登录个性化
};

/**
 * 积分类型
 */
const POINTS_TYPE = {
  CHECKIN: 'CHECKIN',      // 打卡
  EXCHANGE: 'EXCHANGE',    // 兑换
  OTHER: 'OTHER'           // 其他
};

/**
 * 兑换状态
 */
const EXCHANGE_STATUS = {
  PENDING: 0,    // 待发货
  SHIPPED: 1,    // 已发货
  COMPLETED: 2   // 已完成
};

/**
 * 默认配置
 */
const DEFAULT_CONFIG = {
  TARGET_WATER: 2000,  // 默认目标水量（ml）
  POINTS_PER_100ML: 1, // 每100ml获得的积分
  MAX_POINTS_PER_DAY: 50  // 每日最大积分
};

module.exports = {
  API,
  STORAGE_KEY,
  POINTS_TYPE,
  EXCHANGE_STATUS,
  DEFAULT_CONFIG
};

