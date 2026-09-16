Component({
  options: { styleIsolation: 'shared' },
  properties: {
    current: { type: Number, value: 0 },
    max: { type: Number, value: 1000 },
    /** 填充条样式，如 linear-gradient(90deg, #4CAF50 0%, #8BC34A 100%) */
    fillStyle: {
      type: String,
      value: 'linear-gradient(90deg, #4CAF50 0%, #8BC34A 100%)'
    }
  },
  data: {
    percent: 0
  },
  observers: {
    'current, max': function (cur, max) {
      const c = Number(cur) || 0;
      const m = Number(max) || 1;
      this.setData({ percent: Math.min(100, Math.round((c / m) * 100)) });
    }
  }
});
