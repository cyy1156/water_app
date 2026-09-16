// packageOther/mall/mall.js
const request = require('../../../utils/request.js');
const constants = require('../../../utils/constants.js');

function normalizeGoodsImageUrl(url) {
  if (!url) return '';
  let s = String(url).trim();
  if (s.indexOf('hhttps://') === 0 || s.indexOf('hhttp://') === 0) s = s.substring(1);
  if (s.indexOf('//') === 0) return 'https:' + s;
  if (/^https?:\/\//i.test(s)) return s;
  const apiBaseUrl = (getApp().globalData && getApp().globalData.apiBaseUrl) ? String(getApp().globalData.apiBaseUrl) : '';
  const base = apiBaseUrl.replace(/\/+$/, '');
  if (!base) return '';
  if (s.charAt(0) === '/') return base + s;
  return base + '/' + s.replace(/^\/+/, '');
}

Page({
  data: { userPoints: 0, goodsList: [] },
  onLoad() { this.loadData(); },
  onShow() { this.loadUserPoints(); },
  async loadData() {
    await Promise.all([this.loadUserPoints(), this.loadGoodsList()]);
  },
  async loadUserPoints() {
    try {
      const points = await request.get(constants.API.USER_POINTS);
      this.setData({ userPoints: points || 0 });
    } catch (error) {
      if (error.code === 401) {
        await getApp().login();
        this.loadUserPoints();
      }
    }
  },
  async loadGoodsList() {
    try {
      wx.showLoading({ title: '加载中...' });
      const goodsListRaw = await request.get(constants.API.STORE_GOODS_LIST);
      const goodsList = (goodsListRaw || []).map(item => {
        const imageUrl = normalizeGoodsImageUrl(item && item.imageUrl);
        const priceCoin = item.priceCoin != null ? item.priceCoin : (item.pointsRequired || 0);
        return Object.assign({}, item, { imageUrl, priceCoin });
      });
      this.setData({ goodsList: goodsList || [] });
    } catch (error) {
      if (error.code === 401) {
        await getApp().login();
        this.loadGoodsList();
        return;
      }
      wx.showToast({ title: error.message || '加载失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },
  exchangeGoods(e) {
    const goodsId = e.currentTarget.dataset.id;
    const goods = this.data.goodsList.find(item => item.id === goodsId);
    if (!goods) return;
    const priceCoin = goods.priceCoin != null ? goods.priceCoin : (goods.pointsRequired || 0);
    if (this.data.userPoints < priceCoin) {
      wx.showToast({ title: '金币不足', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '确认兑换',
      content: `确定用 ${priceCoin} 金币兑换「${goods.name}」吗？兑换后需在成就里点击「完成」才会计入成就。`,
      success: (res) => { if (res.confirm) this.doExchange(goodsId); }
    });
  },
  async doExchange(goodsId, remindTime) {
    try {
      wx.showLoading({ title: '兑换中...' });
      const body = { goodsId };
      if (remindTime) body.remindTime = remindTime;
      const res = await request.post(constants.API.STORE_PURCHASE, body);
      const recordId = res && res.recordId;
      await Promise.all([this.loadUserPoints(), this.loadGoodsList()]);
      wx.hideLoading();
      wx.showModal({
        title: '兑换成功',
        content: '请确认你已完成这次自我奖励～点击「完成」后才会计入成就。',
        confirmText: '完成',
        cancelText: '稍后',
        success: (r) => {
          if (r.confirm && recordId) this.confirmDone(recordId);
          else if (r.cancel) wx.showToast({ title: '可稍后到成就页点击完成', icon: 'none' });
        }
      });
    } catch (error) {
      wx.hideLoading();
      if (error.code === 401) { await getApp().login(); return; }
      wx.showToast({ title: error.message || '兑换失败', icon: 'none', duration: 2000 });
    }
  },
  async confirmDone(recordId) {
    try {
      await request.post(constants.API.STORE_CONFIRM_DONE, { recordId });
      wx.showToast({ title: '已计入成就～', icon: 'success' });
      wx.showModal({
        title: '已完成',
        content: '已计入成就，可在成就页查看。',
        showCancel: true,
        cancelText: '留在商城',
        confirmText: '去成就',
        success: (r) => {
          if (r.confirm) wx.navigateTo({ url: '/packageOther/achievement/achievement' });
        }
      });
    } catch (e) {
      wx.showToast({ title: e.message || '操作失败', icon: 'none' });
    }
  }
});
