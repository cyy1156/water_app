// pages/home/home.js 粥粥成长页
const request = require('../../utils/request.js');
const constants = require('../../utils/constants.js');
// 主包使用 util.js，让主包“未使用 JS 文件”校验通过
const util = require('../../utils/util.js');

const THEME_STORAGE_KEY = 'themeName';

// 简单主题配置：可根据需要继续扩展色值
const THEMES = {
  green: {
    name: 'green',
    primary: '#4CAF50',
    ringColor: '#4CAF50',
    ringTrack: 'rgba(76,175,80,0.18)',
    expGradient: 'linear-gradient(90deg, #4CAF50 0%, #8BC34A 100%)',
    buttonGradient: 'linear-gradient(135deg, #4CAF50 0%, #8BC34A 100%)',
    outlineBorder: 'rgba(76,175,80,0.28)',
    outlineBg: 'rgba(76,175,80,0.10)',
    outlineText: '#2e7d32',
    // 喝水品种按钮选中状态
    waterTypeBtnActiveBg: 'rgba(76,175,80,0.18)',
    waterTypeBtnActiveColor: '#2e7d32',
    waterTypeBtnActiveBorder: 'rgba(76,175,80,0.4)'
  },
  blue: {
    name: 'blue',
    primary: '#2196F3',
    ringColor: '#2196F3',
    ringTrack: 'rgba(33,150,243,0.10)',
    expGradient: 'linear-gradient(90deg, #2196F3 0%, #64B5F6 100%)',
    buttonGradient: 'linear-gradient(135deg, #1E88E5 0%, #64B5F6 100%)',
    outlineBorder: 'rgba(33,150,243,0.15)',
    outlineBg: 'rgba(33,150,243,0.08)',
    outlineText: '#1976D2',
    // 喝水品种按钮选中状态
    waterTypeBtnActiveBg: 'rgba(33,150,243,0.18)',
    waterTypeBtnActiveColor: '#1976D2',
    waterTypeBtnActiveBorder: 'rgba(33,150,243,0.4)'
  },
  purple: {
    name: 'purple',
    primary: '#9C27B0',
    ringColor: '#9C27B0',
    ringTrack: 'rgba(156,39,176,0.10)',
    expGradient: 'linear-gradient(90deg, #9C27B0 0%, #BA68C8 100%)',
    buttonGradient: 'linear-gradient(135deg, #8E24AA 0%, #BA68C8 100%)',
    outlineBorder: 'rgba(156,39,176,0.15)',
    outlineBg: 'rgba(156,39,176,0.08)',
    outlineText: '#7B1FA2',
    // 喝水品种按钮选中状态
    waterTypeBtnActiveBg: 'rgba(156,39,176,0.18)',
    waterTypeBtnActiveColor: '#7B1FA2',
    waterTypeBtnActiveBorder: 'rgba(156,39,176,0.4)'
  }
};

function clamp(n, min, max) {
  return Math.max(min, Math.min(max, n));
}

function safeNumber(v, fallback = 0) {
  const n = Number(v);
  return Number.isFinite(n) ? n : fallback;
}

function mapStageFromLevel(level) {
  const lv = safeNumber(level, 1);
  if (lv <= 3) return '幼苗';
  if (lv <= 7) return '成长中';
  return '活力粥粥';
}

// 根据当日喝水进度返回一条可爱的流行文案
function getCuteWaterPhrase(progress) {
  const p = clamp(Number(progress) || 0, 0, 1);
  if (p === 0) {
    return '今天还一口没喝呢，先来试试第一口水叭～';
  }
  if (p < 0.25) {
    return '开局一小口，水分正在加载中…💧';
  }
  if (p < 0.5) {
    return '喝到一半啦，继续保持，身体会偷偷给你加分～';
  }
  if (p < 0.75) {
    return '快到 3/4 进度条啦，再喝几口就要变成水润小达人～';
  }
  if (p < 1) {
    return '目标近在眼前，再补一口就可以对自己说「今天很棒」啦！';
  }
  return '今日喝水已满格✅ 身体已经收到你的爱啦～';
}

Page({
  data: {
    // 背景/封面
    coverImage: '',

    // 用户状态
    level: 1,
    exp: 0,
    nextLevelExp: 1000,
    coin: 0,
    stage: '幼苗',
    expPercent: 0,

    // 今日喝水
    currentWater: 0,
    targetWater: 2000,
    // 进度圈：百分比 & 角度
    progress: 0,
    progressDeg: 0,

    // 粥粥占位文案
    chatPlaceholder: '今天喝水了吗？💧',
    tipText: '点一下按钮，粥粥就给你加经验和金币～',

    // 3D 智能体：为 true 且 glbPath 存在时圆环内用 xr-frame 渲染 GLB；否则用 2D 图
    useAgent3D: false,
    glbPath: '/models/agent_default.glb',

    // 主题
    themeName: 'green',
    theme: THEMES.green,
    agentImageUrl: '',  // 从 globalData 同步，用 URL 不打包大图

    // 完整聊天面板
    showChatPanel: false,
    chatMessages: [],
    chatInput: '',
    chatSending: false,
    chatSessionId: '',
    chatLoading: false,
    lastMsgId: '',
    chatScrollTop: 0,  // M3：用于 scroll-view 滚动到底部（每次+1 触发滚动）
    selectedWaterType: 'WATER',  // M5：选中的喝水品种
    showFirstLoginModal: false,
    showFirstLoginPlan: false,
    firstLoginPlan: null,
    // 首次登录：仅昵称输入，留空则随机分配
    firstLoginNickname: ''
  },

  onLoad() {
    this.syncTheme();
    this.setData({ agentImageUrl: getApp().globalData.agentImageUrl || '' });
    this.loadSummary();
    // M3：从 storage 恢复聊天会话 ID，切换 Tab 后会话不丢失
    const savedSessionId = wx.getStorageSync(constants.STORAGE_KEY.CHAT_SESSION_ID) || '';
    if (savedSessionId) {
      this.setData({ chatSessionId: savedSessionId });
    }
    // M5：首次登录个性化
    this.checkFirstLogin();
  },

  onShow() {
    this.syncTheme();
    this.setData({ agentImageUrl: getApp().globalData.agentImageUrl || '' });
    // 返回页面时刷新
    this.loadSummary();
    this.checkFirstLogin();
    // 如果有会话ID且聊天面板打开，加载历史消息
    if (this.data.showChatPanel && this.data.chatSessionId) {
      this.loadChatHistory();
    }
    // 订阅消息授权需要用户手势触发（建议在「我的」页面开关/按钮中操作）
  },

  syncTheme() {
    const app = getApp();
    const stored = wx.getStorageSync(THEME_STORAGE_KEY);
    const name = stored || (app.globalData && app.globalData.themeName) || 'green';
    this.applyTheme(name);
  },

  applyTheme(name) {
    const theme = THEMES[name] || THEMES.green;
    const app = getApp();
    app.globalData.themeName = theme.name;
    wx.setStorageSync(THEME_STORAGE_KEY, theme.name);
    this.setData({
      themeName: theme.name,
      theme
    });
    // 同步更新底部 tabBar 选中文字颜色，让「成长 / 我的」跟随主题色变化
    if (wx && typeof wx.setTabBarStyle === 'function') {
      try {
        wx.setTabBarStyle({
          selectedColor: theme.primary
        });
      } catch (e) {
        // 某些场景（非 tabBar 页面）可能不支持，静默忽略
      }
    }
  },

  async loadSummary() {
    // 优先新接口 /home/summary；若后端未升级，则回退用旧接口拼装
    try {
      const data = await request.get(constants.API.HOME_SUMMARY);
      this.applySummary(data);
    } catch (e) {
      // 401 自动登录由 request.js/页面自行处理；这里做兼容回退
      if (e && e.code === 401) {
        try {
          await getApp().login();
          // 登录成功后立即检查是否需弹出首次登录昵称弹框（用户刚初始化时）
          this.checkFirstLogin();
          return this.loadSummary();
        } catch (err) {}
      }
      await this.loadSummaryFallback();
    }
  },

  async loadSummaryFallback() {
    try {
      const userInfo = await request.get(constants.API.USER_INFO);
      const today = await request.get(constants.API.CHECKIN_TODAY);
      const coin = safeNumber(userInfo.totalPoints, 0); // 旧字段：totalPoints 先当 coin 用
      const level = safeNumber(userInfo.level, 1);
      const exp = safeNumber(userInfo.exp, 0);
      const nextLevelExp = safeNumber(userInfo.nextLevelExp, 1000);
      const targetWater = safeNumber(today.targetWater, 2000);
      const currentWater = safeNumber(today.currentWater, 0);

      this.applySummary({
        user: {
          level,
          exp,
          nextLevelExp,
          coin,
          stage: userInfo.growthStage || userInfo.growth_stage || mapStageFromLevel(level)
        },
        today: {
          currentWater,
          targetWater
        },
        tips: {
          chatPlaceholder: (userInfo.coverText && String(userInfo.coverText).trim()) ? userInfo.coverText : '今天喝水了吗？💧'
        },
        coverImage: userInfo.coverImage || ''
      });
    } catch (e) {
      console.error('首页数据回退加载失败', e);
    }
  },

  applySummary(data) {
    const user = (data && data.user) ? data.user : {};
    const today = (data && data.today) ? data.today : {};
    const tips = (data && data.tips) ? data.tips : {};

    const level = safeNumber(user.level, 1);
    const exp = safeNumber(user.exp, 0);
    const nextLevelExp = safeNumber(user.nextLevelExp, 1000);
    const coin = safeNumber(user.coin, safeNumber(user.totalPoints, 0));
    const stage = user.stage || user.growthStage || mapStageFromLevel(level);

    const currentWater = safeNumber(today.currentWater, 0);
    const targetWater = safeNumber(today.targetWater, 2000);
    const progress = targetWater > 0 ? clamp(currentWater / targetWater, 0, 1) : 0;
    const progressDeg = Math.round(progress * 360);

    const expPercent = nextLevelExp > 0 ? clamp((exp / nextLevelExp) * 100, 0, 100) : 0;

    const defaultPlaceholder = this.data.chatPlaceholder || '今天喝水了吗？💧';
    const cutePlaceholder = getCuteWaterPhrase(progress) || defaultPlaceholder;

    this.setData({
      coverImage: (data && data.coverImage) ? data.coverImage : (this.data.coverImage || ''),
      level,
      exp,
      nextLevelExp,
      coin,
      stage,
      expPercent: Math.round(expPercent),
      currentWater,
      targetWater,
      progress,
      progressDeg,
      // 若后端有自定义文案则优先使用，否则根据当前喝水进度自动生成一条可爱文案
      chatPlaceholder: tips.chatPlaceholder || cutePlaceholder
    });
  },

  chooseTheme() {
    const that = this;
    wx.showActionSheet({
      itemList: ['绿色', '蓝色', '紫色'],
      success(res) {
        const idx = res.tapIndex;
        const map = ['green', 'blue', 'purple'];
        that.applyTheme(map[idx] || 'green');
      }
    });
  },

  selectWaterType(e) {
    const type = e.currentTarget.dataset.type || 'WATER';
    this.setData({ selectedWaterType: type });
  },

  quickAdd(e) {
    const ml = safeNumber(e.currentTarget.dataset.ml, 0);
    if (ml <= 0) return;
    this.submitCheckin(ml);
  },

  onQuickCheckin(e) {
    const ml = e.detail && e.detail.waterMl;
    if (ml != null && ml > 0) {
      this.submitCheckin(ml);
    }
  },

  customAdd() {
    wx.showModal({
      title: '自定义喝水量',
      content: '请输入本次喝水量(ml):',
      editable: true,
      placeholderText: '例如：200',
      success: (res) => {
        if (res.confirm && res.content) {
          const num = parseInt(String(res.content).replace(/[^0-9]/g, ''), 10);
          if (!num || num <= 0) {
            wx.showToast({ title: '请输入有效的喝水量', icon: 'none' });
            return;
          }
          this.submitCheckin(num);
        }
      }
    });
  },

  async submitCheckin(waterMl) {
    const waterType = this.data.selectedWaterType || 'WATER';
    // 优先新接口 /home/checkin；后端未升级则回退到 /checkin/record
    try {
      const result = await request.post(constants.API.HOME_CHECKIN, { waterMl, waterType });
      // 新接口返回聚合结果
      this.applyCheckinResult(result, waterMl);
    } catch (e) {
      if (e && e.code === 401) {
        await getApp().login();
        return this.submitCheckin(waterMl);
      }
      try {
        await request.post(constants.API.CHECKIN_RECORD, { waterMl });
        wx.showToast({ title: '打卡成功', icon: 'success' });
        this.loadSummary(); // 刷新旧接口数据
      } catch (err) {
        console.error('打卡失败', err);
      }
    }
  },

  applyCheckinResult(result, waterMl) {
    if (!result) {
      wx.showToast({ title: '打卡成功', icon: 'success' });
      return this.loadSummary();
    }

    // 兼容 result 结构：{delta,user,today}
    const delta = result.delta || {};
    const user = result.user || {};
    const today = result.today || {};

    const leveledUp = !!user.leveledUp;
    const triggered = !!(delta.targetBonus && delta.targetBonus.triggered);

    // 先更新 UI
    this.applySummary({
      user: {
        level: user.level,
        exp: user.exp,
        nextLevelExp: user.nextLevelExp,
        coin: user.coin,
        stage: user.stage
      },
      today: {
        currentWater: today.currentWater,
        targetWater: today.targetWater
      },
      tips: {
        chatPlaceholder: this.data.chatPlaceholder
      },
      coverImage: this.data.coverImage
    });

    // 再提示
    const expAdd = safeNumber(delta.exp, 0);
    const coinAdd = safeNumber(delta.coin, 0);
    let msg = `+${waterMl}ml  已获得 +${expAdd}EXP +${coinAdd}金币`;
    if (triggered) msg = '达标奖励触发啦！粥粥给你撒花～';
    if (leveledUp) msg = `升级成功！Lv.${user.level} 的你更闪闪发光了～`;
    wx.showToast({ title: msg, icon: 'none', duration: 2000 });
  },

  openChat() {
    const showPanel = !this.data.showChatPanel;
    this.setData({ showChatPanel: showPanel });
    
    if (showPanel) {
      // 打开面板时，如果有会话ID，加载历史消息（loadChatHistory 内部会滚动到底部）
      if (this.data.chatSessionId) {
        this.loadChatHistory();
      } else if (this.data.chatMessages.length > 0) {
        // 已有消息但无 sessionId，滚动到底部
        this.setData({ chatScrollTop: 99999 });
      }
    }
  },

  async loadChatHistory() {
    if (!this.data.chatSessionId || this.data.chatLoading) {
      return;
    }

    this.setData({ chatLoading: true });
    try {
      const history = await request.get(constants.API.CHAT_HISTORY, {
        sessionId: this.data.chatSessionId,
        limit: 50
      });

      if (history && history.length > 0) {
        const messages = history.map(msg => ({
          id: msg.id || Date.now() + Math.random(),
          role: msg.role,
          content: msg.content,
          imageUrl: msg.imageUrl,
          createTime: msg.createTime
        }));

        this.setData({
          chatMessages: messages,
          lastMsgId: messages.length > 0 ? `msg-${messages[messages.length - 1].id}` : '',
          chatScrollTop: 99999  // M3：加载历史后滚动到底部
        });
      }
    } catch (e) {
      console.error('加载聊天历史失败', e);
      if (e && e.code === 401) {
        await getApp().login();
        return this.loadChatHistory();
      }
    } finally {
      this.setData({ chatLoading: false });
    }
  },

  onChatInput(e) {
    this.setData({
      chatInput: e.detail.value
    });
  },

  /**
   * 一次性订阅模拟长期提醒：用户打开小程序时请求授权，授权后立即发送提醒
   * 节流：同一用户 6 小时内最多请求一次
   * 测试时可将 SUBSCRIBE_INTERVAL_MS 改为 1 * 60 * 1000（1分钟）以便快速测试
   */
  async requestSubscribeAndSendRemind() {
    // 微信订阅消息授权通常要求用户手势触发；这里保留函数但不再在 onShow 自动调用
    const SUBSCRIBE_INTERVAL_MS = 6 * 60 * 60 * 1000; // 6 小时（测试时可改为 1 * 60 * 1000）
    const lastKey = constants.STORAGE_KEY.LAST_SUBSCRIBE_REQUEST;

    try {
      const settings = await request.get(constants.API.AI_REMINDER_SETTINGS);
      if (!settings || !settings.enabled || !settings.templateId) return;

      const last = wx.getStorageSync(lastKey) || 0;
      if (Date.now() - last < SUBSCRIBE_INTERVAL_MS) return;

      const templateId = settings.templateId;
      const res = await new Promise((resolve) => {
        wx.requestSubscribeMessage({
          tmplIds: [templateId],
          success: (r) => resolve(r),
          fail: (e) => resolve(e)
        });
      });

      wx.setStorageSync(lastKey, Date.now());

      const accepted = res && res[templateId] === 'accept';
      if (accepted) {
        await request.post(constants.API.AI_REMINDER_SEND_NOW);
        wx.showToast({ title: '粥粥提醒已发送～', icon: 'success' });
        }
    } catch (e) {
      if (e && e.code === 401) return;
      console.error('请求订阅提醒失败', e);
    }
  },

  async sendChat() {
    const text = String(this.data.chatInput || '').trim();
    if (!text) {
      wx.showToast({ title: '要说点什么嘛～', icon: 'none' });
      return;
    }
    if (this.data.chatSending) return;

    // 先添加用户消息到列表
    const userMsg = {
      id: Date.now(),
      role: 'user',
      content: text,
      createTime: new Date().toISOString()
    };
    this.setData({
      chatMessages: this.data.chatMessages.concat(userMsg),
      chatInput: '',
      chatSending: true,
      lastMsgId: `msg-${userMsg.id}`,
      chatScrollTop: 99999  // M3：滚动到底部
    });

    try {
      wx.showLoading({
        title: 'AI思考中...',
        mask: true
      });

      const resp = await request.post(constants.API.CHAT_SEND, {
        sessionId: this.data.chatSessionId || undefined,
        content: text
      });

      if (resp && resp.sessionId) {
        this.setData({ chatSessionId: resp.sessionId });
        // M3：持久化会话 ID，切换 Tab 后不丢失
        wx.setStorageSync(constants.STORAGE_KEY.CHAT_SESSION_ID, resp.sessionId);
      }

      // 添加AI回复消息
      if (resp && resp.messages && resp.messages.length > 0) {
        const newMessages = resp.messages.map(msg => ({
          id: msg.id || Date.now() + Math.random(),
          role: msg.role,
          content: msg.content,
          imageUrl: msg.imageUrl,
          createTime: msg.createTime
        }));

        // 过滤掉已存在的用户消息（避免重复）
        const filteredNew = newMessages.filter(nm => {
          return !this.data.chatMessages.some(existing => 
            existing.role === 'user' && existing.content === nm.content && 
            Math.abs(new Date(existing.createTime) - new Date(nm.createTime)) < 1000
          );
        });

        this.setData({
          chatMessages: this.data.chatMessages.concat(filteredNew),
          lastMsgId: filteredNew.length > 0 ? `msg-${filteredNew[filteredNew.length - 1].id}` : this.data.lastMsgId,
          // M3：强制 scroll-view 滚动到底部
          chatScrollTop: 99999
        });

        // 更新占位文案（使用最后一条AI回复）
        const last = filteredNew.find(m => m.role === 'assistant');
        if (last && last.content) {
          this.setData({ chatPlaceholder: String(last.content).slice(0, 28) });
        }
      }
    } catch (e) {
      console.error('发送聊天失败', e);
      // 移除用户消息（发送失败）
      this.setData({
        chatMessages: this.data.chatMessages.filter(m => m.id !== userMsg.id)
      });
      
      if (e && e.code === 401) {
        await getApp().login();
        this.setData({ chatInput: text });
        return this.sendChat();
      }
      wx.showToast({ title: '粥粥走神了…再试一次嘛', icon: 'none' });
    } finally {
      wx.hideLoading();
      this.setData({ chatSending: false });
    }
  },

  closeChatPanel() {
    this.setData({ showChatPanel: false });
  },

  /** M5：首次登录个性化方案（仅昵称，头像固定为首页模型图） */
  checkFirstLogin() {
    const done = wx.getStorageSync(constants.STORAGE_KEY.FIRST_LOGIN_DONE);
    if (done) return;
    const token = wx.getStorageSync('token');
    if (!token) return;
    this.setData({
      showFirstLoginModal: true,
      firstLoginNickname: ''
    });
  },

  onFirstLoginNicknameInput(e) {
    const nick = (e.detail && e.detail.value) || '';
    this.setData({ firstLoginNickname: nick.trim() });
  },

  onFirstLoginModalTap() {
    // 阻止点击弹框内容时关闭弹框，输入框可正常获得焦点
  },
  onFirstLoginInputFocus() {},
  onFirstLoginInputBlur() {},

  async onFirstLoginSubmit() {
    const that = this;
    let nickName = (this.data.firstLoginNickname || '').trim();
    if (!nickName) {
      nickName = that._getRandomNickname();
    }
    const finalNickName = nickName;

    try {
      wx.showLoading({ title: '提交中...', mask: true });
      const plan = await request.post(constants.API.USER_FIRST_LOGIN, {
        avatarUrl: '',
        nickName: finalNickName,
        gender: 0
      });
      wx.hideLoading();
      try {
        const app = getApp();
        const merged = Object.assign({}, (app.globalData && app.globalData.userInfo) || {}, { nickname: finalNickName });
        app.globalData.userInfo = merged;
        wx.setStorageSync('userInfo', merged);
      } catch (_) {}
      that.setData({
        showFirstLoginModal: false,
        showFirstLoginPlan: true,
        firstLoginPlan: plan
      });
    } catch (e) {
      wx.hideLoading();
      if (e && e.code === 401) {
        try {
          await getApp().login();
          return that.onFirstLoginSubmit();
        } catch (_) {}
      }
      wx.showToast({ title: '获取方案失败', icon: 'none' });
    }
  },

  _getRandomNickname() {
    const pool = ['小可爱', '小水滴', '小粥友', '喝水达人', '活力宝宝', '水灵灵', '咕咚咕咚', '每日一杯', '健康小主', '粥粥好友'];
    const base = pool[Math.floor(Math.random() * pool.length)];
    return base + Math.floor(Math.random() * 10000);
  },

  async onFirstLoginSkip() {
    const randomNickname = this._getRandomNickname();
    try {
      await request.put(constants.API.USER_UPDATE, { nickname: randomNickname });
      const app = getApp();
      const merged = Object.assign({}, (app.globalData && app.globalData.userInfo) || {}, { nickname: randomNickname });
      if (app.globalData) app.globalData.userInfo = merged;
      wx.setStorageSync('userInfo', merged);
    } catch (_) {
      // 未登录或网络失败时仅关闭弹框，昵称保持后端原样
    }
    wx.setStorageSync(constants.STORAGE_KEY.FIRST_LOGIN_DONE, true);
    this.setData({ showFirstLoginModal: false });
  },

  async onFirstLoginAccept() {
    const plan = this.data.firstLoginPlan;
    if (!plan) {
      this.setData({ showFirstLoginPlan: false });
      wx.setStorageSync(constants.STORAGE_KEY.FIRST_LOGIN_DONE, true);
      return;
    }
    try {
      await request.post(constants.API.USER_SETTINGS_TARGET, {
        targetWater: plan.recommendedTarget || 2000
      });
      wx.setStorageSync(constants.STORAGE_KEY.FIRST_LOGIN_DONE, true);
      this.setData({ showFirstLoginPlan: false });
      this.loadSummary();
      wx.showToast({ title: '方案已应用～粥粥陪你一起喝水！', icon: 'success' });
    } catch (e) {
      wx.showToast({ title: '保存失败', icon: 'none' });
    }
  },

  onFirstLoginCustom() {
    wx.setStorageSync(constants.STORAGE_KEY.FIRST_LOGIN_DONE, true);
    this.setData({ showFirstLoginPlan: false });
    wx.switchTab({ url: '/pages/profile/profile' });
    wx.showToast({ title: '去个人中心自定义设置吧～', icon: 'none' });
  }
});


