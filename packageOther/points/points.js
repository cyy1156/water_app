// packageOther/points/points.js
const request = require('../../../utils/request.js');
const constants = require('../../../utils/constants.js');
const util = require('../../../utils/util.js');

Page({
  data: {
    statistics: { totalPoints: 0, totalEarned: 0, totalSpent: 0 },
    list: []
  },
  onLoad() { this.loadAll(); },
  onPullDownRefresh() { this.loadAll().finally(() => wx.stopPullDownRefresh()); },
  async loadAll() {
    try {
      wx.showLoading({ title: '加载中...' });
      await Promise.all([this.loadStatistics(), this.loadList()]);
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },
  async loadStatistics() {
    try {
      const data = await request.get(constants.API.POINTS_STATISTICS);
      this.setData({
        statistics: {
          totalPoints: data.totalPoints || 0,
          totalEarned: data.totalEarned || 0,
          totalSpent: data.totalSpent || 0
        }
      });
    } catch (error) {}
  },
  async loadList() {
    try {
      const list = await request.get(constants.API.POINTS_LIST);
      const mapped = (list || []).map(item => ({
        id: item.id,
        points: item.points,
        type: item.type,
        description: item.description || this.mapTypeToDesc(item.type),
        createTime: item.createTime ? util.formatTime(new Date(item.createTime)) : ''
      }));
      this.setData({ list: mapped });
    } catch (error) {}
  },
  mapTypeToDesc(type) {
    if (!type) return '积分变动';
    switch (type) {
      case 'CHECKIN': return '喝水打卡';
      case 'EXCHANGE': return '积分兑换';
      default: return '其他';
    }
  }
});
