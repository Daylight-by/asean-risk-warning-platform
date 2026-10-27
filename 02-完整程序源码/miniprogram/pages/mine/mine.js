const app = getApp();
const api = require('../../utils/api');
const i18n = require('../../utils/i18n');

Page({
  data: {
    userInfo: null,
    userId: '',
    stats: {
      warningCount: 0,
      subscribeCount: 0,
      policyCount: 0
    },
    cacheSize: ''
  },

  onLoad() {
    this.refreshLanguage();
    this.loadUserInfo();
    this.loadStats();
    this.loadCacheSize();
  },

  onShow() {
    this.refreshLanguage();
    // 节流：距上次加载 < 60s 不重复请求，避免切 tab 时反复刷新闪屏
    const now = Date.now();
    if (this._lastStatsLoad && now - this._lastStatsLoad < 60000) return;
    this.loadStats();
  },

  refreshLanguage() {
    const language = i18n.getLanguage();
    this.setData({ i18n: i18n.getMessages(), languageLabel: i18n.languages.find(item => item.key === language).label });
  },

  onChooseLanguage() {
    i18n.chooseLanguage(() => {
      this.refreshLanguage();
      wx.showToast({ title: i18n.getMessages().mine.changed, icon: 'success' });
    });
  },

  loadUserInfo() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {};
    const userId = wx.getStorageSync('openid') || 'guest_' + Date.now().toString(36);
    this.setData({ userInfo, userId });

    // 不自动调用已废弃的 wx.getUserInfo，避免隐私授权与兼容性问题。
    // 当前页面只需访客标识，昵称头像属于可选展示信息。
    if (!userInfo.nickName) this.setMockUserInfo();
  },

  setMockUserInfo() {
    const userInfo = { nickName: i18n.getMessages().mine.merchant };
    this.setData({ userInfo });
  },

  async loadStats() {
    // 获取上次查看预警的时间戳
    const lastViewWarningTime = wx.getStorageSync('lastViewWarningTime') || 0;

    // 优先从后端获取真实数据
    try {
      const [overviewRes, policyRes] = await Promise.all([
        api.getDashboardOverview(),
        api.getPolicyCount()
      ]);

      const overview = overviewRes.data || overviewRes;
      const policyData = policyRes.data || policyRes;

      const subscribeList = wx.getStorageSync('subscribeList') || [];
      let warningCount = overview.warningCount || 0;

      // 如果用户已经查看过预警页，且后端没有返回未读数，则显示为0（已读）
      if (lastViewWarningTime > 0 && !overview.unreadCount && !overview.newWarningCount) {
        warningCount = 0;
      }

      // 政策新增数 = 当前总数 - 上次查看时的总数（最少为0）
      const currentPolicyTotal = policyData.total || 0;
      // 首次加载：将当前总数记为基准，不显示为新增
      if (!wx.getStorageSync('lastPolicyTotal')) {
        wx.setStorageSync('lastPolicyTotal', currentPolicyTotal);
      }
      const lastPolicyTotal = wx.getStorageSync('lastPolicyTotal') || 0;
      const newPolicyCount = Math.max(0, currentPolicyTotal - lastPolicyTotal);

      this.setData({
        stats: {
          warningCount: warningCount,
          subscribeCount: subscribeList.length,
          policyCount: newPolicyCount
        }
      });
      this._lastStatsLoad = Date.now();
    } catch (err) {
      // 后端不可用，使用模拟数据
      const subscribeList = wx.getStorageSync('subscribeList') || [];
      let warningCount = 5;
      // 已查看过则清零
      if (lastViewWarningTime > 0) {
        warningCount = 0;
      }
      this.setData({
        stats: {
          warningCount: warningCount,
          subscribeCount: subscribeList.length,
          policyCount: 0
        }
      });
      this._lastStatsLoad = Date.now();
    }
  },

  loadCacheSize() {
    try {
      const res = wx.getStorageInfoSync();
      const size = res.currentSize;
      if (size < 1024) {
        this.setData({ cacheSize: size + 'KB' });
      } else {
        this.setData({ cacheSize: (size / 1024).toFixed(1) + 'MB' });
      }
    } catch (e) {
      this.setData({ cacheSize: '0KB' });
    }
  },

  onGoWarning() {
    // 标记已查看预警
    wx.setStorageSync('lastViewWarningTime', Date.now());
    // 先清零角标，再跳转
    this.setData({ 'stats.warningCount': 0 });
    wx.switchTab({ url: '/pages/warning/warning' });
  },

  async onGoPolicy() {
    // 记录查看时间戳和当前政策总数，返回时新增数清零
    try {
      const policyRes = await api.getPolicyCount();
      const policyData = policyRes.data || policyRes;
      wx.setStorageSync('lastPolicyTotal', policyData.total || 0);
    } catch (e) {
      // 获取失败不影响跳转
    }
    wx.setStorageSync('lastViewPolicyTime', Date.now());
    wx.switchTab({ url: '/pages/policy/policy' });
  },

  onGoHistory() {
    const history = wx.getStorageSync('searchHistory') || [];
    wx.showModal({
      title: '搜索历史',
      content: history.length > 0 ? history.join('\n') : '暂无搜索历史',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  onGoSubscribe() {
    wx.navigateTo({ url: '/pages/subscribe/subscribe' });
  },

  onGoAbout() {
    wx.showModal({
      title: '关于平台',
      content: '东盟跨境中小商户智能风控预警平台\n\n本平台以脱敏样例数据演示企业风控评分、信用报告及政策查询流程。\n\n拟接入广西及东盟公开数据，正式接入后逐项登记来源与授权依据。\n\nVersion 1.0.0',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  onClearCache() {
    wx.showModal({
      title: '清除缓存',
      content: '确认清除本地缓存数据？（不会删除您的订阅信息）',
      success: (res) => {
        if (res.confirm) {
          // 保留订阅信息
          const subscribeList = wx.getStorageSync('subscribeList');
          const openid = wx.getStorageSync('openid');
          const token = wx.getStorageSync('token');

          wx.clearStorageSync();

          if (subscribeList) wx.setStorageSync('subscribeList', subscribeList);
          if (openid) wx.setStorageSync('openid', openid);
          if (token) wx.setStorageSync('token', token);

          api.clearCache();

          this.setData({ cacheSize: '0KB' });
          wx.showToast({ title: '缓存已清除', icon: 'success' });
        }
      }
    });
  },

  onShareAppMessage() {
    return {
      title: '东盟风控预警平台 · 跨境商户风控管理',
      path: '/pages/mine/mine'
    };
  }
});
