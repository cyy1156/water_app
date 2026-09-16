// packageOther/calendar/calendar.js
// 注意：此页面在 packageOther 子包下，相对路径只需要返回两级即可
const request = require('../../utils/request.js');
const util = require('../../utils/util.js');
const constants = require('../../utils/constants.js');

const THEME_STORAGE_KEY = 'themeName';
const THEMES = {
  green: { primary: '#4CAF50', monthBtnBg: '#4CAF50', dayCheckedBg: 'rgba(76,175,80,0.35)', dayCheckedColor: '#2e7d32', dayHighBg: 'rgba(76,175,80,0.55)', dayHighColor: '#1b5e20', dayVeryHighBg: 'rgba(76,175,80,0.75)', dayVeryHighColor: '#fff' },
  blue: { primary: '#2196F3', monthBtnBg: '#2196F3', dayCheckedBg: 'rgba(33,150,243,0.35)', dayCheckedColor: '#1565C0', dayHighBg: 'rgba(33,150,243,0.55)', dayHighColor: '#0d47a1', dayVeryHighBg: 'rgba(33,150,243,0.75)', dayVeryHighColor: '#fff' },
  purple: { primary: '#9C27B0', monthBtnBg: '#9C27B0', dayCheckedBg: 'rgba(156,39,176,0.35)', dayCheckedColor: '#7B1FA2', dayHighBg: 'rgba(156,39,176,0.55)', dayHighColor: '#4A148C', dayVeryHighBg: 'rgba(156,39,176,0.75)', dayVeryHighColor: '#fff' }
};
const WATER_TYPE_LABEL = { WATER: '白水', TEA: '茶', COFFEE: '咖啡', MILK_TEA: '奶茶', DRINK: '饮料' };

Page({
  data: {
    currentYear: new Date().getFullYear(),
    currentMonth: new Date().getMonth() + 1,
    calendarDays: [],
    selectedDay: null,
    monthSummary: null,
    theme: THEMES.green
  },
  onLoad() { this.syncTheme(); this.loadCalendar(); },
  onShow() { this.syncTheme(); },
  syncTheme() {
    const stored = wx.getStorageSync(THEME_STORAGE_KEY);
    const name = stored || (getApp().globalData && getApp().globalData.themeName) || 'green';
    this.setData({ theme: THEMES[name] || THEMES.green });
  },
  async loadCalendar() {
    try {
      const { currentYear, currentMonth } = this.data;
      try {
        const res = await request.get(constants.API.CALENDAR_STATS, { year: currentYear, month: currentMonth });
        if (res && res.dailyStats) {
          this.generateCalendarFromStats(res.dailyStats, res.monthSummary);
          return;
        }
      } catch (e) {
        if (e && e.code === 401) {
          await getApp().login();
          return this.loadCalendar();
        }
      }
      const data = await request.get(constants.API.CHECKIN_CALENDAR, { year: currentYear, month: currentMonth });
      this.generateCalendar(data || []);
    } catch (error) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },
  generateCalendarFromStats(dailyStats, monthSummary) {
    const { currentYear, currentMonth } = this.data;
    const statMap = {};
    (dailyStats || []).forEach(item => {
      if (item.date) statMap[item.date] = { checkinCount: item.checkinCount || 0, totalWater: item.totalWater || 0 };
    });
    const firstDay = new Date(currentYear, currentMonth - 1, 1);
    const firstDayWeek = firstDay.getDay();
    const lastDay = new Date(currentYear, currentMonth, 0);
    const daysInMonth = lastDay.getDate();
    const days = [];
    for (let i = 0; i < firstDayWeek; i++) days.push({ day: '', date: '', checkinCount: 0, totalWater: 0 });
    for (let day = 1; day <= daysInMonth; day++) {
      const monthStr = currentMonth < 10 ? '0' + currentMonth : String(currentMonth);
      const dayStr = day < 10 ? '0' + day : String(day);
      const date = `${currentYear}-${monthStr}-${dayStr}`;
      const stat = statMap[date] || { checkinCount: 0, totalWater: 0 };
      days.push({ day, date, checkinCount: stat.checkinCount, totalWater: stat.totalWater, checked: stat.checkinCount > 0 });
    }
    this.setData({ calendarDays: days, monthSummary: monthSummary || null });
  },
  generateCalendar(backendDays) {
    const { currentYear, currentMonth } = this.data;
    const checkedMap = {};
    (backendDays || []).forEach(item => { if (item.date) checkedMap[item.date] = !!item.checked; });
    const firstDay = new Date(currentYear, currentMonth - 1, 1);
    const firstDayWeek = firstDay.getDay();
    const lastDay = new Date(currentYear, currentMonth, 0);
    const daysInMonth = lastDay.getDate();
    const days = [];
    for (let i = 0; i < firstDayWeek; i++) days.push({ day: '', date: '', checked: false, checkinCount: 0 });
    for (let day = 1; day <= daysInMonth; day++) {
      const monthStr = currentMonth < 10 ? '0' + currentMonth : String(currentMonth);
      const dayStr = day < 10 ? '0' + day : String(day);
      const date = `${currentYear}-${monthStr}-${dayStr}`;
      days.push({ day, date, checked: !!checkedMap[date], checkinCount: checkedMap[date] ? 1 : 0 });
    }
    this.setData({ calendarDays: days, monthSummary: null });
  },
  prevMonth() {
    let { currentYear, currentMonth } = this.data;
    currentMonth--; if (currentMonth < 1) { currentMonth = 12; currentYear--; }
    this.setData({ currentYear, currentMonth }); this.loadCalendar();
  },
  nextMonth() {
    let { currentYear, currentMonth } = this.data;
    currentMonth++; if (currentMonth > 12) { currentMonth = 1; currentYear++; }
    this.setData({ currentYear, currentMonth }); this.loadCalendar();
  },
  async selectDay(e) {
    const date = e.currentTarget.dataset.date;
    if (!date) return;
    try {
      const list = await request.get(constants.API.CHECKIN_HISTORY, { date });
      const records = (list || []).map(item => ({
        id: item.id,
        time: item.time || (item.checkinTime ? util.formatTime(new Date(item.checkinTime)) : ''),
        ml: item.ml != null ? item.ml : item.waterMl,
        waterTypeLabel: WATER_TYPE_LABEL[item.waterType] || item.waterType
      }));
      this.setData({ selectedDay: { date, records } });
    } catch (error) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  }
});
