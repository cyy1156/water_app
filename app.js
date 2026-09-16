// app.js
App({
  // 云能力初始化的单例 Promise（避免在生命周期里重复触发）
  _cloudInitPromise: null,

  onLaunch() {
    // 小程序启动时执行
    // 保持 onLaunch 极简：仅做同步逻辑，避免触发生命周期等待态
    console.log('小程序启动（仅同步逻辑）');
  },

  onShow() {
    // 小程序显示时执行
  },

  onHide() {
    // 小程序隐藏时执行
  },

  /**
   * 检查登录状态
   */
  checkLogin() {
    const token = wx.getStorageSync('token');
    if (!token) {
      // 如果没有token，执行登录
      this.login();
    } else {
      // 验证token是否有效（可选：调用后端验证接口）
      console.log('已登录');
    }
  },

  /**
   * 微信登录
   */
  login() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: async (res) => {
          if (res.code) {
            try {
              // 发送code到后端换取token
              const request = require('./utils/request.js');
              const constants = require('./utils/constants.js');
              
              const loginData = await request.post(constants.API.USER_LOGIN, {
                code: res.code
              });

              // 保存token和用户信息
              wx.setStorageSync('token', loginData.token);
              this.globalData.token = loginData.token;
              this.globalData.userInfo = loginData.userInfo;

              console.log('登录成功');
              resolve(loginData);
            } catch (error) {
              console.error('登录失败', error);
              reject(error);
            }
          } else {
            console.error('获取code失败', res.errMsg);
            reject(new Error(res.errMsg));
          }
        },
        fail: (err) => {
          console.error('登录失败', err);
          reject(err);
        }
      });
    });
  },

  /**
   * 确保云能力已初始化（供 request.js 在真正发请求前调用）
   * 说明：这里不做登录，只做 wx.cloud.init；登录由页面/401 触发更安全。
   */
  ensureCloudInited() {
    if (this._cloudInitPromise) return this._cloudInitPromise;

    this._cloudInitPromise = new Promise((resolve) => {
      try {
        // 延迟到下一轮事件循环，尽量避免和启动生命周期“等待态”绑在一起
        setTimeout(() => {
          try {
            if (wx.cloud && typeof wx.cloud.init === 'function') {
              wx.cloud.init({
                env: this.globalData.cloudEnvId, // 以全局环境 ID 为准
                traceUser: true
              });
              resolve(true);
              return;
            }
          } catch (e) {
            // ignore
          }
          resolve(false);
        }, 0);
      } catch (e) {
        resolve(false);
      }
    });

    return this._cloudInitPromise;
  },

  /**
   * 全局数据
   */
  globalData: {
    userInfo: null,
    token: null,
    // 配置API地址：本地开发用 localhost，云托管测试用下方云地址
    apiBaseUrl: 'https://springboot-mezu-226383-7-1405114852.sh.run.tcloudbase.com/api', // 云托管
    // apiBaseUrl: 'http://localhost:8080/api',  // 本地开发时改回此项
    // 主题：green / blue / purple
    themeName: 'green',
    // 智能体/占位图用 URL，不打包进代码包，满足「图片和音频资源≤200K」
    // 填完整 URL 如 https://xxx.com/agent.png；留空则首页圆环内不显示图、首次登录占位不显示图
    agentImageUrl: 'https://image2url.com/r2/default/images/1771840797728-2d2e01e1-3dc1-425c-93da-6314384ede13.png',
    // 云托管服务名：wx.cloud.callContainer 需要
    // 注意：这里填“服务名称” springboot-mezu，而不是版本名 springboot-mezu-011
    // 云环境 ID（给 callContainer 显式指定，避免环境切换导致路由失败）
    cloudEnvId: 'prod-4g7jm41l0631d7a2',
    containerService: 'springboot-mezu',
    // 是否优先走云托管内网访问
    preferCallContainer: true
  }
});

