const api = require('../../utils/api');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    keyword: '',
    loading: false,
    graph: null
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  async onSearch() {
    const kw = this.data.keyword.trim();
    if (!kw) {
      wx.showToast({ title: '请输入企业名称', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const res = await api.getRelationGraph(kw);
      if (res.code === 500 || res.code === 404) {
        this.loadMock(kw);
      } else {
        this.setData({ graph: res.data || res });
      }
    } catch (err) {
      this.loadMock(kw);
    } finally {
      this.setData({ loading: false });
    }
  },

  // 接口不可用时本地演示（中-东盟跨境多层关联网络）
  loadMock(name) {
    this.setData({
      graph: {
        nodes: [
          { layer: '国内主体', name: name, country: '中国·广西', risk: 'high', note: '疑似空壳，实缴资本为0' },
          { layer: '跨境代理', name: '越泰国际货运代理(凭祥)', country: '中国·广西', risk: 'medium', note: '频繁变更法人' },
          { layer: '东盟离岸', name: 'VIET TRADE CO., LTD', country: '越南·河内', risk: 'high', note: '境外代持法人，注册地虚拟' },
          { layer: '上游供货', name: '胡志明市XX农产品公司', country: '越南·胡志明', risk: 'low', note: '真实贸易背景' }
        ],
        links: [
          '国内空壳 + 东盟离岸企业联合套利',
          '虚假转口贸易（货物流与资金流背离）',
          '跨境资金分流至境外账户'
        ],
        summary: '识别到「国内空壳公司 + 东盟离岸企业」联合套利链路，建议重点监控跨境资金流与转口贸易真实性。'
      }
    });
  },

  onShareAppMessage() {
    return { title: '东盟风控预警平台 · 跨境关联图谱', path: '/pages/graph/graph' };
  }
});
