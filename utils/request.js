// utils/request.js - 网络请求封装
const TOKEN_KEY = 'token';
let loadingCount = 0;
let cloudInitPromise = null;
let cloudInited = false;

function getBaseUrl() {
  try {
    const app = getApp();
    return (app && app.globalData && app.globalData.apiBaseUrl)
      ? String(app.globalData.apiBaseUrl)
      : 'https://your-api-domain.com/api';
  } catch (_) {
    return 'https://your-api-domain.com/api';
  }
}

function shouldUseCallContainer() {
  try {
    const app = getApp();
    const prefer = !!(app && app.globalData && app.globalData.preferCallContainer);
    return prefer && !!(wx.cloud && wx.cloud.callContainer);
  } catch (_) {
    return !!(wx.cloud && wx.cloud.callContainer);
  }
}

function getContainerService() {
  try {
    const app = getApp();
    return (app && app.globalData && app.globalData.containerService)
      ? String(app.globalData.containerService)
      : '';
  } catch (_) {
    return '';
  }
}

function getCloudEnvId() {
  try {
    const app = getApp();
    return (app && app.globalData && app.globalData.cloudEnvId)
      ? String(app.globalData.cloudEnvId)
      : '';
  } catch (_) {
    return '';
  }
}

function ensureWxCloudInit() {
  if (cloudInited) return Promise.resolve(true);
  if (cloudInitPromise) return cloudInitPromise;

  cloudInitPromise = new Promise((resolve) => {
    try {
      if (wx.cloud && typeof wx.cloud.init === 'function') {
        const envId = getCloudEnvId();
        // wx.cloud.init 本身是同步的，但必须在调用任何 cloud API 前执行一次
        wx.cloud.init({
          env: envId || undefined,
          traceUser: true
        });
        cloudInited = true;
        resolve(true);
        return;
      }
    } catch (e) {
      // ignore
    }
    resolve(false);
  });

  return cloudInitPromise;
}

function normalizeContainerPath(url) {
  const u = String(url || '').trim();
  if (!u) return '/api';
  // 你的后端 context-path 是 /api，所以内网调用默认补上 /api 前缀
  if (u.indexOf('/api/') === 0 || u === '/api') return u;
  if (u.charAt(0) === '/') return '/api' + u;
  return '/api/' + u;
}

class Request {
  /**
   * 统一请求方法
   * @param {string} url - 请求路径
   * @param {string} method - 请求方法
   * @param {object} data - 请求数据
   * @returns {Promise} 请求结果
   */
  request(url, method = 'GET', data = {}) {
    return new Promise((resolve, reject) => {
      const token = wx.getStorageSync(TOKEN_KEY);
      loadingCount += 1;
      if (loadingCount === 1) {
        wx.showLoading({
          title: '加载中...',
          mask: true
        });
      }

      const hideGlobalLoading = () => {
        loadingCount = Math.max(0, loadingCount - 1);
        if (loadingCount === 0) {
          wx.hideLoading();
        }
      };

      const doWxRequest = () => {
        const BASE_URL = getBaseUrl();
        wx.request({
          url: BASE_URL + url,
          method,
          data,
          header: {
            'Content-Type': 'application/json',
            'Authorization': token ? `Bearer ${token}` : ''
          },
          timeout: 60000,
          success: (res) => {
            hideGlobalLoading();
            if (res.statusCode === 200) {
              if (res.data && res.data.code === 200) {
                resolve(res.data.data);
              } else if (res.data && res.data.code === 401) {
                this.handleTokenExpired();
                reject(res.data);
              } else {
                const errorMsg = res.data ? (res.data.message || '请求失败') : '服务器返回格式错误';
                wx.showToast({ title: errorMsg, icon: 'none', duration: 2000 });
                reject(res.data || { code: res.statusCode, message: errorMsg });
              }
            } else if (res.statusCode === 500) {
              wx.showToast({ title: '服务器错误，请检查后端服务', icon: 'none', duration: 3000 });
              reject({ code: 500, message: '服务器内部错误' });
            } else {
              wx.showToast({ title: `请求失败：${res.statusCode}`, icon: 'none' });
              reject({ code: res.statusCode, message: '请求失败' });
            }
          },
          fail: (err) => {
            hideGlobalLoading();
            let errorMsg = '网络错误，请检查网络连接';
            if (BASE_URL.includes('your-api-domain.com')) {
              errorMsg = '请先配置API地址（app.js中的apiBaseUrl）';
            } else if (err.errMsg && err.errMsg.includes('fail')) {
              errorMsg = '无法连接到服务器，请检查后端服务是否启动';
            }
            wx.showToast({ title: errorMsg, icon: 'none', duration: 3000 });
            console.error('请求失败详情：', err);
            reject(err);
          }
        });
      };

      // 1) 优先走云托管内网调用（手机预览/线上更稳定，不依赖 request 合法域名）
      if (shouldUseCallContainer()) {
        const service = getContainerService();
        const path = normalizeContainerPath(url);
        const envId = getCloudEnvId();
        ensureWxCloudInit().finally(() => {
          // 无论 init 是否成功，都尝试一次 callContainer；失败再按原逻辑回退 wx.request
          wx.cloud.callContainer({
            config: envId ? { env: envId } : undefined,
            path,
            method,
            data,
            header: {
              'X-WX-SERVICE': service,
              'Content-Type': 'application/json',
              'Authorization': token ? `Bearer ${token}` : ''
            }
          }).then((res) => {
            hideGlobalLoading();
            const statusCode = res && (res.statusCode || res.status) ? (res.statusCode || res.status) : 0;
            const body = res && res.data ? res.data : {};

            if (statusCode === 200) {
              if (body && body.code === 200) {
                resolve(body.data);
                return;
              }
              if (body && body.code === 401) {
                this.handleTokenExpired();
                reject(body);
                return;
              }
              const errorMsg = body ? (body.message || '请求失败') : '服务器返回格式错误';
              wx.showToast({ title: errorMsg, icon: 'none', duration: 2000 });
              reject(body || { code: statusCode, message: errorMsg });
              return;
            }

            wx.showToast({ title: `请求失败：${statusCode || '网络错误'}`, icon: 'none' });
            reject({ code: statusCode || -1, message: '请求失败' });
          }).catch((err) => {
            // callContainer 失败：常见原因是服务名/环境配置错误（INVALID_HOST）
            // 为了不影响主流程，这里自动回退到 wx.request（公网域名）继续尝试
            const errMsg = (err && (err.errMsg || err.message)) ? String(err.errMsg || err.message) : '';
            const invalidHost = errMsg.includes('INVALID_HOST') || errMsg.includes('Invalid host') || (err && err.errCode === -501000);
            console.error('callContainer 失败详情：', err);
            if (invalidHost) {
              doWxRequest();
              return;
            }
            hideGlobalLoading();
            const errorMsg = '无法连接到云托管服务，请检查云能力初始化和服务名';
            wx.showToast({ title: errorMsg, icon: 'none', duration: 3000 });
            reject(err);
          });
        });
        return;
      }

      // 2) 兜底：仍可走公网域名（开发者工具/本地调试）
      doWxRequest();
    });
  }

  /**
   * Token过期处理
   */
  handleTokenExpired() {
    wx.removeStorageSync(TOKEN_KEY);
    wx.showModal({
      title: '提示',
      content: '登录已过期，请重新登录',
      showCancel: false,
      success: () => {
        // 重新登录
        getApp().login();
      }
    });
  }

  /**
   * GET请求
   * @param {string} url - 请求路径
   * @param {object} data - 请求参数
   * @returns {Promise} 请求结果
   */
  get(url, data = {}) {
    return this.request(url, 'GET', data);
  }

  /**
   * POST请求
   * @param {string} url - 请求路径
   * @param {object} data - 请求数据
   * @returns {Promise} 请求结果
   */
  post(url, data = {}) {
    return this.request(url, 'POST', data);
  }

  /**
   * PUT请求
   * @param {string} url - 请求路径
   * @param {object} data - 请求数据
   * @returns {Promise} 请求结果
   */
  put(url, data = {}) {
    return this.request(url, 'PUT', data);
  }

  /**
   * DELETE请求
   * @param {string} url - 请求路径
   * @param {object} data - 请求数据
   * @returns {Promise} 请求结果
   */
  delete(url, data = {}) {
    return this.request(url, 'DELETE', data);
  }
}

module.exports = new Request();

