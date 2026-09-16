Component({
  properties: {
    text: {
      type: String,
      value: ''
    },
    /** 智能体图片 URL（不打包本地图，满足代码包图片≤200K） */
    agentImageUrl: {
      type: String,
      value: ''
    }
  },

  data: {
    displayText: ''
  },

  observers: {
    text(val) {
      const str = (val || '').trim();
      // 统一限制长度，避免在小壳里撑爆
      this.setData({
        displayText: str.length > 28 ? str.slice(0, 28) + '…' : str || '今天喝水了吗？💧'
      });
    }
  },

  methods: {
    onTap() {
      this.triggerEvent('tapAgent');
    }
  }
});


