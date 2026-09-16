Component({
  options: { styleIsolation: 'shared' },
  properties: {
    text: { type: String, value: '今天喝水了吗？💧' }
  },
  data: {
    displayText: ''
  },
  observers: {
    text(val) {
      const str = (val || '').trim();
      this.setData({
        displayText: str.length > 28 ? str.slice(0, 28) + '…' : str || '今天喝水了吗？💧'
      });
    }
  },
  methods: {
    onTap() {
      this.triggerEvent('tap');
    }
  }
});
