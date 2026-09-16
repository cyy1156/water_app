// packageOther/chat/chat.js
const request = require('../../../utils/request.js');
const constants = require('../../../utils/constants.js');

Page({
  data: {
    messages: [],
    inputText: '',
    sessionId: '',
    loading: false,
    showInput: false
  },
  onLoad() {
    wx.redirectTo({ url: '/pages/home/home' });
  },
  onShow() {
    if (this.data.sessionId) this.loadHistory();
  },
  async loadHistory() {
    if (!this.data.sessionId) return;
    try {
      const history = await request.get(constants.API.CHAT_HISTORY, { sessionId: this.data.sessionId, limit: 50 });
      if (history && history.length > 0) {
        const messages = history.map(msg => ({
          id: msg.id || Date.now() + Math.random(),
          role: msg.role,
          content: msg.content,
          imageUrl: msg.imageUrl,
          createTime: msg.createTime
        }));
        this.setData({ messages });
        setTimeout(() => wx.pageScrollTo({ scrollTop: 99999, duration: 0 }), 100);
      }
    } catch (error) {
      if (error.code === 401) {
        await getApp().login();
        this.loadHistory();
      }
    }
  },
  showInputDialog() { this.setData({ showInput: true }); },
  hideInputDialog() {
    setTimeout(() => {
      if (!this.data.inputText.trim()) this.setData({ showInput: false });
    }, 200);
  },
  onInput(e) { this.setData({ inputText: e.detail.value }); },
  async sendMessage() {
    const text = this.data.inputText.trim();
    if (!text) { wx.showToast({ title: '请输入消息', icon: 'none' }); return; }
    if (this.data.loading) return;
    this.setData({ inputText: '', loading: true, showInput: false });
    try {
      wx.showLoading({ title: 'AI思考中...', mask: true });
      const response = await request.post(constants.API.CHAT_SEND, {
        sessionId: this.data.sessionId || undefined,
        content: text
      });
      if (response.sessionId) this.setData({ sessionId: response.sessionId });
      if (response.messages && response.messages.length > 0) {
        const newMessages = response.messages.map(msg => ({
          id: msg.id || Date.now() + Math.random(),
          role: msg.role,
          content: msg.content,
          imageUrl: msg.imageUrl,
          createTime: msg.createTime
        }));
        this.setData({
          messages: [...this.data.messages, ...newMessages],
          lastMsgId: `msg-${newMessages[newMessages.length - 1].id}`
        });
        setTimeout(() => wx.pageScrollTo({ scrollTop: 99999, duration: 300 }), 100);
      }
    } catch (error) {
      if (error.code === 401) {
        await getApp().login();
        this.setData({ inputText: text });
        this.sendMessage();
        return;
      }
      wx.showToast({ title: error.message || '发送失败，请重试', icon: 'none', duration: 2000 });
    } finally {
      wx.hideLoading();
      this.setData({ loading: false });
    }
  }
});
