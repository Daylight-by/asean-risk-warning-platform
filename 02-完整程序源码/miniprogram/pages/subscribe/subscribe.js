const api = require('../../utils/api');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    list: []
  },

  onShow() {
    this.loadList();
  },

  async loadList() {
    const localList = wx.getStorageSync('subscribeList') || [];
    this.setData({ list: localList });
    try {
      const res = await api.getSubscribedList();
      const remoteList = (res && res.data) || [];
      if (Array.isArray(remoteList)) {
        wx.setStorageSync('subscribeList', remoteList);
        this.setData({ list: remoteList });
      }
    } catch (err) {
      // 后端不可用时保留本地订阅列表
    }
  },

  onView(e) {
    const { id, name } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/score/score?enterpriseId=${id}&enterpriseName=${encodeURIComponent(name)}`
    });
  },

  onUnsubscribe(e) {
    const { id, name } = e.currentTarget.dataset;
    wx.showModal({
      title: '取消订阅',
      content: `确认取消对「${name}」的预警订阅？`,
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await api.unsubscribeWarning(id);
        } catch (err) {
          // 接口不可用时仍更新本地状态
        }
        let list = wx.getStorageSync('subscribeList') || [];
        list = list.filter(it => it.enterpriseId !== id);
        wx.setStorageSync('subscribeList', list);
        this.setData({ list });
        wx.showToast({ title: '已取消订阅', icon: 'success' });
      }
    });
  },

  onShareAppMessage() {
    return {
      title: '东盟风控预警平台 · 我的订阅',
      path: '/pages/subscribe/subscribe'
    };
  }
});
