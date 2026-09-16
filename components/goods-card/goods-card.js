Component({
  options: { styleIsolation: 'shared' },
  properties: {
    goods: {
      type: Object,
      value: null
    },
    /** 用户金币是否足够购买该商品 */
    coinEnough: {
      type: Boolean,
      value: false
    },
    /** 是否正在购买（禁用按钮） */
    buying: {
      type: Boolean,
      value: false
    },
    /** 购买按钮背景色（足够时） */
    buyBtnBg: {
      type: String,
      value: 'linear-gradient(135deg, #4CAF50 0%, #8BC34A 100%)'
    },
    placeholderBg: {
      type: String,
      value: 'rgba(0,0,0,0.06)'
    }
  },
  methods: {
    onBuy() {
      if (this.properties.buying || !this.properties.coinEnough) return;
      const g = this.properties.goods;
      if (g && g.id) {
        this.triggerEvent('buy', { goodsId: g.id });
      }
    }
  }
});
