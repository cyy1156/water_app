// pages/profile/profile.js
const request = require('../../utils/request.js');
const constants = require('../../utils/constants.js');

const THEME_STORAGE_KEY = 'themeName';
const WX_PROFILE_PROMPT_KEY = 'wxProfilePromptAt';
const PROFILE_FIRST_GUIDE_KEY = 'profileFirstGuideDone';
const REMIND_SUBSCRIBED_KEY = 'remindSubscribedOnce';

// 主题配置（与home页面一致）
const THEMES = {
  green: {
    name: 'green',
    primary: '#4CAF50',
    expGradient: 'linear-gradient(90deg, #4CAF50 0%, #8BC34A 100%)',
    pillBg: 'rgba(76,175,80,0.12)',
    pillBorder: 'rgba(76,175,80,0.22)',
    pillText: '#2e7d32',
    buyBtnBg: '#4CAF50',
    // 文字颜色
    textPrimary: '#4CAF50',
    textLink: '#4CAF50',
    btnBg: 'rgba(76,175,80,0.12)',
    btnBorder: 'rgba(76,175,80,0.3)',
    btnText: '#4CAF50',
    // 头像占位符
    avatarPlaceholderBg: 'linear-gradient(135deg, rgba(76,175,80,0.25) 0%, rgba(139,195,74,0.25) 100%)',
    // 商品图片占位符
    goodsPlaceholderBg: 'linear-gradient(135deg, rgba(76,175,80,0.18) 0%, rgba(139,195,74,0.18) 100%)'
  },
  blue: {
    name: 'blue',
    primary: '#2196F3',
    expGradient: 'linear-gradient(90deg, #2196F3 0%, #64B5F6 100%)',
    pillBg: 'rgba(33,150,243,0.12)',
    pillBorder: 'rgba(33,150,243,0.22)',
    pillText: '#1976D2',
    buyBtnBg: '#2196F3',
    // 文字颜色
    textPrimary: '#2196F3',
    textLink: '#2196F3',
    btnBg: 'rgba(33,150,243,0.12)',
    btnBorder: 'rgba(33,150,243,0.3)',
    btnText: '#2196F3',
    // 头像占位符
    avatarPlaceholderBg: 'linear-gradient(135deg, rgba(33,150,243,0.25) 0%, rgba(100,181,246,0.25) 100%)',
    // 商品图片占位符
    goodsPlaceholderBg: 'linear-gradient(135deg, rgba(33,150,243,0.18) 0%, rgba(100,181,246,0.18) 100%)'
  },
  purple: {
    name: 'purple',
    primary: '#9C27B0',
    expGradient: 'linear-gradient(90deg, #9C27B0 0%, #BA68C8 100%)',
    pillBg: 'rgba(156,39,176,0.12)',
    pillBorder: 'rgba(156,39,176,0.22)',
    pillText: '#7B1FA2',
    buyBtnBg: '#9C27B0',
    // 文字颜色
    textPrimary: '#9C27B0',
    textLink: '#9C27B0',
    btnBg: 'rgba(156,39,176,0.12)',
    btnBorder: 'rgba(156,39,176,0.3)',
    btnText: '#9C27B0',
    // 头像占位符
    avatarPlaceholderBg: 'linear-gradient(135deg, rgba(156,39,176,0.25) 0%, rgba(186,104,200,0.25) 100%)',
    // 商品图片占位符
    goodsPlaceholderBg: 'linear-gradient(135deg, rgba(156,39,176,0.18) 0%, rgba(186,104,200,0.18) 100%)'
  }
};

function safeNumber(v, fallback = 0) {
  const n = Number(v);
  return Number.isFinite(n) ? n : fallback;
}

function clamp(n, min, max) {
  return Math.max(min, Math.min(max, n));
}

function mapStageFromLevel(level) {
  const lv = safeNumber(level, 1);
  if (lv <= 3) return '幼苗';
  if (lv <= 7) return '成长中';
  return '活力粥粥';
}

function getGlobalUserInfoFallback() {
  try {
    const app = getApp();
    if (app && app.globalData && app.globalData.userInfo) {
      return app.globalData.userInfo;
    }
  } catch (_) {}
  try {
    const cached = wx.getStorageSync('userInfo');
    return cached || {};
  } catch (_) {
    return {};
  }
}

// 随机昵称池：首次登录无昵称时分配
const RANDOM_NICKNAMES = [
  '小可爱', '小水滴', '小粥友', '喝水达人', '活力宝宝',
  '水灵灵', '咕咚咕咚', '每日一杯', '健康小主', '粥粥好友'
];
function getRandomNickname() {
  const idx = Math.floor(Math.random() * RANDOM_NICKNAMES.length);
  const base = RANDOM_NICKNAMES[idx];
  const suffix = Math.floor(Math.random() * 10000);
  return base + suffix;
}

/**
 * 规范化商品图片地址：支持
 * - 修复手滑输入的 hhttps / hhttp
 * - 绝对地址（http/https、//）原样或补全
 * - 相对地址（/uploads/xxx 或 uploads/xxx）自动拼接后端域名
 */
function normalizeGoodsImageUrl(url) {
  if (!url) return '';
  let s = String(url).trim();

  // 修复常见手滑：hhttps://  / hhttp://
  if (s.indexOf('hhttps://') === 0 || s.indexOf('hhttp://') === 0) {
    s = s.substring(1);
  }

  // //example.com/a.png
  if (s.indexOf('//') === 0) {
    return 'https:' + s;
  }

  // http(s) 绝对地址
  if (/^https?:\/\//i.test(s)) {
    return s;
  }

  // 拼接后端 base（注意：后端配置了 context-path: /api，所以静态资源实际也在 /api 下）
  const apiBaseUrl = (getApp().globalData && getApp().globalData.apiBaseUrl) ? String(getApp().globalData.apiBaseUrl) : '';
  const base = apiBaseUrl.replace(/\/+$/, '');

  if (!base) return '';

  // /uploads/goods/xxx.png 这种以 / 开头的相对地址
  if (s.charAt(0) === '/') {
    return base + s;
  }

  // uploads/goods/xxx.png 这种不带前导 / 的相对地址
  return base + '/' + s.replace(/^\/+/, '');
}

Page({
  data: {
    coverImage: '',
    coverText: '今天也要多喝水哦~',

    // 头像固定为首页模型图（agentImageUrl），不再使用用户上传/微信头像
    profileAvatarUrl: '',
    nickname: '',
    displayName: '小可爱',

    // 成长系统（coin/exp/level）
    level: 1,
    exp: 0,
    nextLevelExp: 1000,
    expPercent: 0,
    coin: 0,
    stage: '幼苗',

    // 主题
    themeName: 'green',
    theme: THEMES.green,

    // 商城
    goodsList: [],
    buying: false,
    buyingGoodsId: null,

    // M4：粥粥提醒
    reminderEnabled: false,
    reminderSaving: false,
    reminderIntervalIndex: 2,  // 默认 90 分钟
    reminderIntervalOptions: [
      { label: '30分钟', value: 30 },
      { label: '60分钟', value: 60 },
      { label: '90分钟', value: 90 },
      { label: '120分钟', value: 120 }
    ],
    // 提醒预览（GET /ai/reminder/preview）
    reminderPreview: null,
    // 提前缓存好的订阅模板ID（用于按钮直接调 wx.requestSubscribeMessage）
    reminderTemplateId: '',
    // M5：每日目标
    targetWater: 2000
  },

  onLoad() {
    console.log('个人中心加载');
    this.syncTheme();
    this.syncProfileAvatarUrl();
    this.loadAll();
  },

  onShow() {
    this.syncTheme();
    this.syncProfileAvatarUrl();
    this.loadAll();
    this.runFirstProfileGuide();
  },

  // 头像固定为首页模型图像
  syncProfileAvatarUrl() {
    try {
      const url = (getApp().globalData && getApp().globalData.agentImageUrl) ? String(getApp().globalData.agentImageUrl).trim() : '';
      if (url && this.data.profileAvatarUrl !== url) {
        this.setData({ profileAvatarUrl: url });
      }
    } catch (_) {}
  },

  // 首次登录无昵称时：随机分配并写回后端
  async ensureNicknameAsync(nickname) {
    const n = String(nickname || '').trim();
    if (n && n !== '微信用户') return n;
    const randomName = getRandomNickname();
    try {
      await request.put(constants.API.USER_UPDATE, { nickname: randomName });
      const app = getApp();
      const merged = Object.assign({}, (app.globalData && app.globalData.userInfo) || {}, { nickname: randomName });
      app.globalData.userInfo = merged;
      wx.setStorageSync('userInfo', merged);
      return randomName;
    } catch (e) {
      if (e && e.code === 401) {
        try {
          await getApp().login();
          return this.ensureNicknameAsync(nickname);
        } catch (_) {}
      }
      return randomName;
    }
  },

  // 点击昵称：用户自己修改昵称
  onTapEditNickname() {
    const that = this;
    wx.showModal({
      title: '修改昵称',
      editable: true,
      placeholderText: '请输入昵称',
      content: this.data.displayName === '小可爱' || !this.data.displayName ? '' : this.data.displayName,
      success: async (r) => {
        if (!r.confirm) return;
        const nick = String(r.content || '').trim();
        if (!nick) {
          wx.showToast({ title: '昵称不能为空', icon: 'none' });
          return;
        }
        if (nick.length > 12) {
          wx.showToast({ title: '昵称最多12个字', icon: 'none' });
          return;
        }
        try {
          await request.put(constants.API.USER_UPDATE, { nickname: nick });
          const app = getApp();
          const merged = Object.assign({}, (app.globalData && app.globalData.userInfo) || {}, { nickname: nick });
          app.globalData.userInfo = merged;
          wx.setStorageSync('userInfo', merged);
          that.setData({ nickname: nick, displayName: nick });
          wx.showToast({ title: '昵称已更新', icon: 'success' });
        } catch (e) {
          if (e && e.code === 401) {
            try {
              await getApp().login();
              that.onTapEditNickname();
            } catch (_) {}
            return;
          }
          wx.showToast({ title: e.message || '保存失败', icon: 'none' });
        }
      }
    });
  },

  syncTheme() {
    const app = getApp();
    const stored = wx.getStorageSync(THEME_STORAGE_KEY);
    const name = stored || (app.globalData && app.globalData.themeName) || 'green';
    this.applyTheme(name);
  },

  applyTheme(name) {
    const theme = THEMES[name] || THEMES.green;
    this.setData({
      themeName: theme.name,
      theme
    });
    // 更新底部 tabBar 选中文字颜色，让「成长 / 我的」跟随主题色变化
    if (wx && typeof wx.setTabBarStyle === 'function') {
      try {
        wx.setTabBarStyle({
          selectedColor: theme.primary
        });
      } catch (e) {
        // 非 tabBar 场景调用会报错，这里忽略
      }
    }
  },

  async loadAll() {
    await Promise.all([
      this.loadMeSummary(),
      this.loadGoods(),
      this.loadReminderSettings()
    ]);
  },

  async loadMeSummary() {
    // 优先新接口 /me/summary；回退用 /user/info + /user/points 拼装
    try {
      const data = await request.get(constants.API.ME_SUMMARY);
      await this.applyMe(data);
      return;
    } catch (e) {
      if (e && e.code === 401) {
        await getApp().login();
        return this.loadMeSummary();
      }
    }

    try {
      const userInfo = await request.get(constants.API.USER_INFO);
      const points = await request.get(constants.API.USER_POINTS);
      const coin = safeNumber(points, safeNumber(userInfo.totalPoints, 0));
      const level = safeNumber(userInfo.level, 1);
      const exp = safeNumber(userInfo.exp, 0);
      const nextLevelExp = safeNumber(userInfo.nextLevelExp, 1000);

      await this.applyMe({
        user: {
          nickname: userInfo.nickname,
          coverImage: userInfo.coverImage,
          coverText: userInfo.coverText,
          level,
          exp,
          nextLevelExp,
          coin,
          stage: userInfo.growthStage || mapStageFromLevel(level)
        }
      });
    } catch (e) {
      console.error('加载我的信息失败', e);
    }
  },

  async applyMe(data) {
    const user = (data && (data.user || data)) ? (data.user || data) : {};
    const globalUser = getGlobalUserInfoFallback();
    const level = safeNumber(user.level, 1);
    const exp = safeNumber(user.exp, 0);
    const nextLevelExp = safeNumber(user.nextLevelExp, 1000);
    const expPercent = nextLevelExp > 0 ? clamp((exp / nextLevelExp) * 100, 0, 100) : 0;
    const targetWater = user.targetWater != null ? user.targetWater : this.data.targetWater || 2000;

    let nickname = user.nickname || globalUser.nickname || this.data.nickname || '';
    // 首次登录无昵称或为默认昵称：随机分配并写回后端
    if (!nickname.trim() || nickname === '微信用户') {
      nickname = await this.ensureNicknameAsync(nickname);
    }
    const displayName = nickname || '小可爱';

    this.setData({
      coverImage: user.coverImage || this.data.coverImage || '',
      coverText: user.coverText || this.data.coverText || '今天也要多喝水哦~',
      nickname,
      displayName,
      level,
      exp,
      nextLevelExp,
      expPercent: Math.round(expPercent),
      coin: safeNumber(user.coin, safeNumber(user.totalPoints, this.data.coin || 0)),
      stage: user.stage || user.growthStage || mapStageFromLevel(level),
      targetWater
    });

    try {
      const app = getApp();
      const merged = Object.assign({}, (app.globalData && app.globalData.userInfo) || {}, { nickname });
      app.globalData.userInfo = merged;
      wx.setStorageSync('userInfo', merged);
    } catch (_) {}
  },

  async loadGoods() {
    // 优先新接口 /store/goods/list；回退到 /mall/goods/list 并把 pointsRequired 当 priceCoin
    try {
      const list = await request.get(constants.API.STORE_GOODS_LIST);
      this.setData({
        goodsList: (list || []).map(g => ({
          id: g.id,
          name: g.name,
          description: g.description,
          imageUrl: normalizeGoodsImageUrl(g.imageUrl),
          // 新接口已返回 priceCoin，这里仅做一次兼容兜底
          priceCoin: safeNumber(g.priceCoin, safeNumber(g.pointsRequired, 0)),
          stock: g.stock
        }))
      });
      return;
    } catch (e) {
      if (e && e.code === 401) {
        await getApp().login();
        return this.loadGoods();
      }
    }

    try {
      const list = await request.get(constants.API.MALL_GOODS_LIST);
      this.setData({
        goodsList: (list || []).map(g => ({
          id: g.id,
          name: g.name,
          description: g.description,
          imageUrl: normalizeGoodsImageUrl(g.imageUrl),
          priceCoin: safeNumber(g.pointsRequired, 0),
          stock: g.stock
        }))
      });
    } catch (e) {
      console.error('加载商品失败', e);
    }
  },

  async loadReminderSettings() {
    try {
      const res = await request.get(constants.API.AI_REMINDER_SETTINGS);
      if (res) {
        const enabled = typeof res.enabled === 'boolean' ? res.enabled : false;
        const intervalMin = res.intervalMin != null ? res.intervalMin : 90;
        const opts = [30, 60, 90, 120];
        let idx = opts.indexOf(intervalMin);
        if (idx < 0) idx = 2;
        this.setData({
          reminderEnabled: enabled,
          reminderIntervalIndex: idx,
          reminderTemplateId: res.templateId || ''
        });
        if (enabled) {
          this.loadReminderPreview();
        } else {
          this.setData({ reminderPreview: null });
        }
      }
    } catch (e) {
      if (e && e.code === 401) {
        await getApp().login();
        return this.loadReminderSettings();
      }
      console.error('加载提醒设置失败', e);
    }
  },

  async loadReminderPreview() {
    try {
      const vo = await request.get(constants.API.AI_REMINDER_PREVIEW);
      this.setData({ reminderPreview: vo });
    } catch (e) {
      this.setData({ reminderPreview: null });
    }
  },

  onReminderIntervalChange(e) {
    const idx = parseInt(e.detail.value, 10);
    const opts = this.data.reminderIntervalOptions;
    const intervalMin = opts[idx] ? opts[idx].value : 90;
    this.setData({ reminderIntervalIndex: idx });
    this.saveReminderInterval(intervalMin);
  },

  async saveReminderInterval(intervalMin) {
    try {
      await request.post(constants.API.AI_REMINDER_SETTINGS, {
        enabled: this.data.reminderEnabled,
        startTime: '09:00',
        endTime: '22:00',
        intervalMin,
        intensity: 'SOFT',
        quietAfterTarget: true
      });
      wx.showToast({ title: '提醒间隔已更新', icon: 'success' });
      if (this.data.reminderEnabled) {
        this.loadReminderPreview();
      }
    } catch (e) {
      wx.showToast({ title: '保存失败', icon: 'none' });
    }
  },

  showTargetSetting() {
    const that = this;
    const quickOptions = [1500, 2000, 2500, 3000];
    wx.showActionSheet({
      itemList: ['1500 ml', '2000 ml', '2500 ml', '3000 ml', '自定义'],
      success(res) {
        if (res.tapIndex === 4) {
          wx.showModal({
            title: '设置目标水量',
            editable: true,
            placeholderText: '输入 500-5000',
            success(r) {
              if (r.confirm && r.content) {
                const n = parseInt(String(r.content).replace(/\D/g, ''), 10);
                if (n >= 500 && n <= 5000) {
                  that.saveTargetWater(n);
                } else {
                  wx.showToast({ title: '请输入 500-5000 之间的数字', icon: 'none' });
                }
              }
            }
          });
        } else {
          const val = quickOptions[res.tapIndex] || 2000;
          that.saveTargetWater(val);
        }
      }
    });
  },

  async saveTargetWater(targetWater) {
    try {
      await request.post(constants.API.USER_SETTINGS_TARGET, { targetWater });
      this.setData({ targetWater });
      wx.showToast({ title: '目标已更新为 ' + targetWater + ' ml', icon: 'success' });
    } catch (e) {
      if (e && e.code === 401) {
        await getApp().login();
        return;
      }
      wx.showToast({ title: e.message || '保存失败', icon: 'none' });
    }
  },

  // 首次进入「我的」页：昵称已在 loadMeSummary -> applyMe 中若无则随机分配，此处仅作占位
  runFirstProfileGuide() {
    try {
      wx.setStorageSync(PROFILE_FIRST_GUIDE_KEY, Date.now());
    } catch (_) {}
  },
  
  // 单独按钮触发订阅授权：真正的 TAP 手势里直接调用 wx.requestSubscribeMessage
  onSubscribeTap() {
    const templateId = (this.data.reminderTemplateId || '').trim();
    if (!templateId) {
      wx.showToast({ title: '后台未配置订阅模板', icon: 'none' });
      return;
    }
    wx.requestSubscribeMessage({
      tmplIds: [templateId],
      success: async (res) => {
        const accepted = res && res[templateId] === 'accept';
        try {
          if (accepted) {
            // 记录本机已成功订阅过一次，供「立即请求提醒」直接发送使用
            wx.setStorageSync(REMIND_SUBSCRIBED_KEY, true);
          }
          await request.post(constants.API.AI_REMINDER_SUBSCRIBE, { accepted });
        } catch (_) {}
        if (accepted) {
          wx.showToast({ title: '订阅成功，粥粥会提醒你喝水～', icon: 'success' });
        } else {
          wx.showToast({ title: '你暂未授权订阅，可在微信设置中打开', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '订阅请求失败，请稍后再试', icon: 'none' });
      }
    });
  },

  async onReminderChange(e) {
    const enabled = e.detail.value;
    if (this.data.reminderSaving) return;

    this.setData({ reminderSaving: true });

    try {
      const finalEnabled = enabled;
      const opts = this.data.reminderIntervalOptions;
      const idx = this.data.reminderIntervalIndex;
      const intervalMin = (opts && opts[idx]) ? opts[idx].value : 90;
      await request.post(constants.API.AI_REMINDER_SETTINGS, {
        enabled: finalEnabled,
        startTime: '09:00',
        endTime: '22:00',
        intervalMin,
        intensity: 'SOFT',
        quietAfterTarget: true
      });

      this.setData({ reminderEnabled: finalEnabled });
      if (finalEnabled) {
        this.loadReminderPreview();
      } else {
        this.setData({ reminderPreview: null });
      }
      wx.showToast({
        title: finalEnabled ? '已开启粥粥提醒～' : '已关闭提醒',
        icon: 'success'
      });
    } catch (err) {
      if (err && err.code === 401) {
        await getApp().login();
        this.setData({ reminderSaving: false });
        return;
      }
      console.error('保存提醒设置失败', err);
      wx.showToast({ title: '设置失败，请重试', icon: 'none' });
      this.setData({ reminderEnabled: !enabled });
    } finally {
      this.setData({ reminderSaving: false });
    }
  },

  /** 一次性订阅：手动请求提醒（请求授权后立即发送） */
  async requestRemindNow() {
    if (this.data.reminderSaving) return;
    if (!this.data.reminderEnabled) return;

    this.setData({ reminderSaving: true });
    try {
      // 若本机已记录“曾经订阅成功”，则不再强制拉起订阅授权，直接请求后端发送一次提醒
      let subscribedOnce = false;
      try {
        subscribedOnce = !!wx.getStorageSync(REMIND_SUBSCRIBED_KEY);
      } catch (_) {}

      if (subscribedOnce) {
        await request.post(constants.API.AI_REMINDER_SEND_NOW);
        wx.showToast({ title: '粥粥提醒已发送～', icon: 'success' });
      } else {
        const settings = await request.get(constants.API.AI_REMINDER_SETTINGS);
        const templateId = settings && settings.templateId ? String(settings.templateId).trim() : '';
        if (!templateId) {
          wx.showToast({ title: '请先在后台配置订阅模板', icon: 'none' });
          return;
        }

        const res = await new Promise((resolve) => {
          wx.requestSubscribeMessage({
            tmplIds: [templateId],
            success: (r) => resolve(r),
            fail: (e) => resolve(e || {})
          });
        });

        const accepted = res && res[templateId] === 'accept';
        if (accepted) {
          try {
            wx.setStorageSync(REMIND_SUBSCRIBED_KEY, true);
          } catch (_) {}
          await request.post(constants.API.AI_REMINDER_SEND_NOW);
          wx.showToast({ title: '粥粥提醒已发送～', icon: 'success' });
        } else {
          wx.showToast({ title: '你暂未授权订阅，可在微信设置中打开', icon: 'none' });
        }
      }
    } catch (e) {
      if (e && e.code === 401) {
        await getApp().login();
        return this.requestRemindNow();
      }
      wx.showToast({ title: '请求失败，请重试', icon: 'none' });
    } finally {
      this.setData({ reminderSaving: false });
    }
  },

  tapFeature(e) {
    const key = e.currentTarget.dataset.key;
    if (key === 'achv') {
      wx.navigateTo({ url: '/packageOther/achievement/achievement' });
      return;
    }
    let title = '功能开发中';
    if (key === 'role') title = '我的角色（开发中）';
    if (key === 'roleStore') title = '角色商城（开发中）';
    if (key === 'growth') title = '成长记录（开发中）';
    wx.showToast({ title, icon: 'none' });
  },

  onGoodsBuy(e) {
    const goodsId = e.detail && e.detail.goodsId;
    if (!goodsId) return;
    this.buyGoods({ currentTarget: { dataset: { id: goodsId } } });
  },

  buyGoods(e) {
    const goodsId = e.currentTarget.dataset.id;
    const goods = (this.data.goodsList || []).find(x => x.id === goodsId);
    if (!goods) return;
    if (this.data.buying) return;

    if (safeNumber(goods.priceCoin, 0) > safeNumber(this.data.coin, 0)) {
      wx.showToast({ title: '金币不足～多喝几口水滴滴就有啦！', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '确认购买',
      content: `确定花 ${goods.priceCoin} 金币购买「${goods.name}」吗？`,
      success: (res) => {
        if (res.confirm) {
          this.doPurchase(goodsId);
        }
      }
    });
  },

  async doPurchase(goodsId) {
    this.setData({ buying: true, buyingGoodsId: goodsId });
    try {
      const res = await request.post(constants.API.STORE_PURCHASE, { goodsId });
      // 优先使用后端返回的剩余金币，避免多一次请求
      if (res && typeof res.remainingCoin === 'number') {
        this.setData({ coin: safeNumber(res.remainingCoin, this.data.coin) });
      }
      wx.showToast({ title: '购买成功～', icon: 'success' });
      await this.loadGoods();
      return;
    } catch (e) {
      // 回退到旧兑换接口（按积分扣减）
      if (e && e.code === 401) {
        await getApp().login();
        this.setData({ buying: false, buyingGoodsId: null });
        return;
      }
      try {
        await request.post(constants.API.MALL_EXCHANGE, { goodsId });
        wx.showToast({ title: '兑换成功～', icon: 'success' });
        await Promise.all([this.loadMeSummary(), this.loadGoods()]);
      } catch (err) {
        console.error('购买/兑换失败', err);
        wx.showToast({ title: '购买失败，请稍后重试', icon: 'none' });
      }
    } finally {
      this.setData({ buying: false, buyingGoodsId: null });
    }
  }
});

