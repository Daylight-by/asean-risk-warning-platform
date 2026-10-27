const api = require('../../utils/api');
const i18n = require('../../utils/i18n');

Page({
  data: {
    keyword: '',
    suggestList: [],
    resultList: [],
    searchHistory: [],
    showResults: false,
    hasSearched: false,
    lastKeyword: '',
    autoFocus: false
  },

  onLoad(options) {
    this.refreshLanguage();
    this.loadHistory();
    // 从首页历史记录带入关键词，自动搜索
    if (options.keyword) {
      const keyword = decodeURIComponent(options.keyword);
      this.setData({ keyword, autoFocus: true });
      this.onSearch();
    } else {
      this.setData({ autoFocus: true });
    }
  },

  onShow() {
    this.refreshLanguage();
    this.loadHistory();
  },

  refreshLanguage() {
    this.setData({ i18n: i18n.getMessages() });
  },

  onUnload() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  },

  // 加载搜索历史
  loadHistory() {
    const history = wx.getStorageSync('searchHistory') || [];
    this.setData({ searchHistory: history });
  },

  // 保存搜索历史
  saveHistory(keyword) {
    let history = wx.getStorageSync('searchHistory') || [];
    history = history.filter(item => item !== keyword);
    history.unshift(keyword);
    if (history.length > 10) history.pop();
    wx.setStorageSync('searchHistory', history);
    this.setData({ searchHistory: history });
  },

  // 输入事件（防抖500ms）
  onInput(e) {
    const keyword = e.detail.value;
    this.setData({ keyword });

    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }

    if (keyword.length >= 2) {
      this.debounceTimer = setTimeout(() => {
        this.fetchSuggest(keyword);
      }, 500);
    } else {
      this.setData({ suggestList: [] });
    }
  },

  // 获取联想结果
  async fetchSuggest(keyword) {
    try {
      const res = await api.searchEnterprise(keyword);
      // 【面试】字段映射：后端返enterpriseName，wxml用item.name，JS层做别名
      const list = (res.data || []).map(it => ({
        ...it,
        name: it.enterpriseName || it.name || ''
      }));
      this.setData({ suggestList: list });
    } catch (err) {
      console.error('搜索联想失败:', err);
    }
  },

  // 搜索
  async onSearch() {
    const keyword = this.data.keyword.trim();
    if (!keyword || keyword.length < 2) {
      wx.showToast({ title: i18n.getMessages().search.minChars, icon: 'none' });
      return;
    }

    this.saveHistory(keyword);
    this.setData({
      suggestList: [],
      lastKeyword: keyword,
      showResults: true
    });

    try {
      const res = await api.searchEnterprise(keyword);
      // 同样做字段映射
      const list = (res.data || []).map(it => ({
        ...it,
        name: it.enterpriseName || it.name || ''
      }));
      this.setData({
        resultList: list,
        hasSearched: true
      });
    } catch (err) {
      this.setData({ hasSearched: true });
    }
  },

  // 选择联想项
  onSelectSuggest(e) {
    const { id, name } = e.currentTarget.dataset;
    this.setData({ keyword: name, suggestList: [] });
    this.saveHistory(name);
    wx.navigateTo({ url: `/pages/score/score?enterpriseId=${id}&enterpriseName=${encodeURIComponent(name)}` });
  },

  // 选择搜索结果
  onSelectResult(e) {
    const { id } = e.currentTarget.dataset;
    const item = this.data.resultList.find(r => r.id === id);
    const name = item ? (item.enterpriseName || item.name || '') : '';
    wx.navigateTo({ url: `/pages/score/score?enterpriseId=${id}&enterpriseName=${encodeURIComponent(name)}` });
  },

  // 点击历史记录
  onTapHistory(e) {
    const keyword = e.currentTarget.dataset.keyword;
    this.setData({ keyword });
    this.onSearch();
  },

  // 清除输入
  onClear() {
    this.setData({ keyword: '', suggestList: [], showResults: false, hasSearched: false, resultList: [] });
  },

  // 清除历史
  onClearHistory() {
    wx.setStorageSync('searchHistory', []);
    this.setData({ searchHistory: [] });
  }
});
