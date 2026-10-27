// 开发环境地址可在微信开发者工具“本地存储”中设置 apiBaseURL，避免把某台电脑的局域网 IP 写死。
// 例如：apiBaseURL = http://192.168.0.110:8080/api；评委换电脑时只需替换这一项。
const configuredBaseURL = wx.getStorageSync('apiBaseURL');

App({
  onLaunch() {
    const token = wx.getStorageSync('token');
    if (!token) {
      this.autoLogin();
    }
    require('./utils/i18n').applyTabBarLanguage();
  },

  autoLogin() {
    const openid = wx.getStorageSync('openid') || ('guest_' + Date.now());
    wx.setStorageSync('openid', openid);
  },

  globalData: {
    userInfo: null,
    token: null,
    // 模拟器默认访问本机；真机调试请在本地存储设置 apiBaseURL 为电脑局域网地址。
    // 上线时改为已备案 HTTPS 域名，并加入小程序 request 合法域名。
    baseURL: configuredBaseURL || 'http://127.0.0.1:8080/api'
  }
});
