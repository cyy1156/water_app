// packageOther/achievement/achievement.js 成就（两个页面：目标成就 / 兑换记录）
// 注意：本文件在 packageOther 分包下，相对路径只需要返回两级即可
const request = require('../../utils/request.js');
const constants = require('../../utils/constants.js');

const THEME_STORAGE_KEY = 'themeName';

// 主题配置（与首页、个人中心一致，按钮颜色与主题一致）
const THEMES = {
  green: {
    name: 'green',
    primary: '#4CAF50',
    buttonGradient: 'linear-gradient(135deg, #4CAF50 0%, #8BC34A 100%)'
  },
  blue: {
    name: 'blue',
    primary: '#2196F3',
    buttonGradient: 'linear-gradient(135deg, #1E88E5 0%, #64B5F6 100%)'
  },
  purple: {
    name: 'purple',
    primary: '#9C27B0',
    buttonGradient: 'linear-gradient(135deg, #8E24AA 0%, #BA68C8 100%)'
  }
};

function formatTime(d) {
  if (!d) return '';
  const t = new Date(d);
  const y = t.getFullYear();
  const m = String(t.getMonth() + 1).padStart(2, '0');
  const day = String(t.getDate()).padStart(2, '0');
  const h = String(t.getHours()).padStart(2, '0');
  const min = String(t.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day} ${h}:${min}`;
}

Page({
  data: {
    activeTab: 'system', // system | redeem
    systemAchievements: [],
    redeemedAchievements: [],
    pendingList: [],
    theme: THEMES.green
  },

  onLoad() {
    this.syncTheme();
    this.loadList();
    this.loadPending();
  },

  onShow() {
    this.syncTheme();
    this.loadList();
    this.loadPending();
  },

  syncTheme() {
    const stored = wx.getStorageSync(THEME_STORAGE_KEY);
    const name = stored || (getApp().globalData && getApp().globalData.themeName) || 'green';
    const theme = THEMES[name] || THEMES.green;
    this.setData({ theme });
  },

  onSwitchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab) this.setData({ activeTab: tab });
  },

  async loadList() {
    try {
      wx.showLoading({ title: '加载中...' });
      const res = await request.get(constants.API.ACHIEVEMENT_LIST);
      const system = (res && res.systemAchievements) ? res.systemAchievements : [];
      const redeemed = (res && res.redeemedAchievements) ? res.redeemedAchievements : [];
      this.setData({
        systemAchievements: system.map(a => ({
          ...a,
          completedAtStr: formatTime(a.completedAt)
        })),
        redeemedAchievements: redeemed.map(a => ({
          ...a,
          completedAtStr: formatTime(a.completedAt)
        }))
      });
    } catch (e) {
      if (e && e.code === 401) {
        await getApp().login();
        return this.loadList();
      }
      wx.showToast({ title: e.message || '加载失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  async loadPending() {
    try {
      const list = await request.get(constants.API.STORE_PENDING_DONE) || [];
      this.setData({
        pendingList: list.map(item => ({
          ...item,
          createTimeStr: formatTime(item.createTime)
        }))
      });
    } catch (e) {
      if (e && e.code === 401) return;
      this.setData({ pendingList: [] });
    }
  },

  async onConfirmDone(e) {
    const recordId = e.currentTarget.dataset.recordId;
    if (!recordId) return;
    try {
      await request.post(constants.API.STORE_CONFIRM_DONE, { recordId });
      wx.showToast({ title: '已计入成就～', icon: 'success' });
      this.loadList();
      this.loadPending();
    } catch (err) {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    }
  }
});
