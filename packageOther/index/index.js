// packageOther/index/index.js
const request = require('../../../utils/request.js');
const util = require('../../../utils/util.js');
const constants = require('../../../utils/constants.js');

function normalizeNumberInput(value) {
  if (value == null) return '';
  let str = String(value).trim();
  const full = '０１２３４５６７８９';
  const half = '0123456789';
  let result = '';
  for (let ch of str) {
    const idx = full.indexOf(ch);
    if (idx >= 0) result += half[idx];
    else result += ch;
  }
  return result.replace(/[^0-9]/g, '');
}

Page({
  data: { currentWater: 0, targetWater: 2000, historyList: [] },
  onLoad() { this.loadTodayData(); },
  onShow() { this.loadTodayData(); },
  async loadTodayData() {
    try {
      wx.showLoading({ title: '加载中...' });
      const data = await request.get(constants.API.CHECKIN_TODAY);
      const historyList = (data.historyList || []).map(item => ({
        id: item.id,
        time: item.checkinTime ? util.formatTime(new Date(item.checkinTime)) : '',
        ml: item.waterMl
      }));
      this.setData({
        currentWater: data.currentWater || 0,
        targetWater: data.targetWater || 2000,
        historyList
      });
    } catch (error) {
      if (error.code === 401) {
        await getApp().login();
        this.loadTodayData();
      } else {
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    } finally {
      wx.hideLoading();
    }
  },
  handleCheckin() {
    wx.showModal({
      title: '打卡喝水',
      content: '请输入本次喝水量(ml):',
      editable: true,
      placeholderText: '例如：200',
      success: (res) => {
        if (res.confirm && res.content) {
          const ml = parseInt(normalizeNumberInput(res.content), 10);
          if (isNaN(ml) || ml <= 0) {
            wx.showToast({ title: '请输入有效的喝水量', icon: 'none' });
            return;
          }
          this.submitCheckin(ml);
        }
      }
    });
  },
  async submitCheckin(waterMl) {
    try {
      wx.showLoading({ title: '提交中...' });
      const record = await request.post(constants.API.CHECKIN_RECORD, { waterMl });
      const newRecord = {
        id: record.id,
        time: util.formatTime(new Date(record.checkinTime)),
        ml: record.waterMl
      };
      this.setData({
        currentWater: this.data.currentWater + waterMl,
        historyList: [newRecord, ...this.data.historyList]
      });
      wx.showToast({ title: '打卡成功', icon: 'success' });
    } catch (error) {
      if (error.code === 401) {
        await getApp().login();
        this.submitCheckin(waterMl);
      } else {
        wx.showToast({ title: error.message || '打卡失败', icon: 'none' });
      }
    } finally {
      wx.hideLoading();
    }
  }
});
