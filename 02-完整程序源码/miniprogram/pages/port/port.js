const api = require('../../utils/api');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    keyword: '',
    loading: false,
    profile: null
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onClear() {
    this.setData({ keyword: '', profile: null });
  },

  async onSearch() {
    const kw = this.data.keyword.trim();
    if (!kw) {
      wx.showToast({ title: this.data.t.pages.port.required, icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const res = await api.getPortProfile(kw);
      if (res.code === 500 || res.code === 404) {
        this.loadMock(kw);
      } else {
        this.setData({ profile: res.data || res });
      }
    } catch (err) {
      this.loadMock(kw);
    } finally {
      this.setData({ loading: false });
    }
  },

  // 接口不可用时本地演示（边境口岸维度数据）
  loadMock(name) {
    this.setData({
      profile: {
        enterpriseName: name,
        creditCode: '91450681MA5QCDGM2X',
        primaryPort: '东兴口岸',
        ports: [
          { name: '东兴口岸', clearanceAvgDays: 1.2, inspectRate: '2.1%', smallTradeQuota: '¥480万', originCompliance: '合规' },
          { name: '凭祥口岸', clearanceAvgDays: 1.8, inspectRate: '3.4%', smallTradeQuota: '¥260万', originCompliance: '合规' },
          { name: '钦州港', clearanceAvgDays: 2.6, inspectRate: '5.0%', smallTradeQuota: '—', originCompliance: '存疑' }
        ],
        settlementScore: 82,
        settlementLevel: '良好',
        settlementRecords: [
          { label: '跨境人民币收付汇', value: '¥1.2亿', status: '正常' },
          { label: '出口退税', value: '¥860万', status: '正常' },
          { label: '跨境担保', value: '2笔', status: '正常' }
        ],
        riskTags: [
          { text: '口岸查验异常率偏低', level: 'ok' },
          { text: '原产地申报合规', level: 'ok' },
          { text: '钦州港原产地申报存疑1次', level: 'warn' }
        ]
      }
    });
  },

  onShareAppMessage() {
    return { title: '东盟风控预警平台 · 口岸通关画像', path: '/pages/port/port' };
  }
});
