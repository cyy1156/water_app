Component({
  options: { styleIsolation: 'shared' },
  properties: {
    /** 进度 0~1 或 0~100 */
    progress: {
      type: Number,
      value: 0
    },
    /** 进度条颜色（conic-gradient 用，如 #4CAF50） */
    ringColor: {
      type: String,
      value: '#4CAF50'
    },
    /** 轨道颜色 */
    ringTrack: {
      type: String,
      value: 'rgba(0,0,0,0.06)'
    },
    /** 圆环尺寸 rpx */
    size: {
      type: Number,
      value: 560
    }
  },
  data: {
    progressDeg: 0
  },
  observers: {
    progress(v) {
      let p = Number(v);
      if (p > 1) p = p / 100;
      p = Math.max(0, Math.min(1, p));
      this.setData({ progressDeg: Math.round(p * 360) });
    }
  }
});
