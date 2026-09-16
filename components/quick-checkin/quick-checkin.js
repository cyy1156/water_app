Component({
  options: { styleIsolation: 'shared' },
  properties: {
    buttonGradient: { type: String, value: 'linear-gradient(135deg, #4CAF50 0%, #8BC34A 100%)' },
    outlineBorder: { type: String, value: 'rgba(76,175,80,0.5)' },
    outlineBg: { type: String, value: 'transparent' },
    outlineText: { type: String, value: '#4CAF50' },
    tipText: { type: String, value: '' }
  },
  methods: {
    onQuick(e) {
      const ml = e.currentTarget.dataset.ml;
      if (ml != null) {
        this.triggerEvent('checkin', { waterMl: parseInt(ml, 10) });
      }
    },
    onCustom() {
      this.triggerEvent('custom');
    }
  }
});
