const api = require('../../utils/api');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    loading: true,
    heatmap: [],
    clusters: []
  },

  onLoad() {
    this.loadData();
  },

  onPullDownRefresh() {
    this.loadData().then(() => wx.stopPullDownRefresh());
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [hRes, cRes] = await Promise.all([
        api.getRegulateHeatmap(),
        api.getRegulateClusters()
      ]);
      const heat = (hRes.data && hRes.data.list) || hRes.data || hRes || null;
      const clu = (cRes.data && cRes.data.list) || cRes.data || cRes || null;
      if (!heat || (hRes.code === 500 || hRes.code === 404)) {
        this.setData({ heatmap: this.mockHeatmap() });
      } else {
        this.setData({ heatmap: heat });
      }
      if (!clu || (cRes.code === 500 || cRes.code === 404)) {
        this.setData({ clusters: this.mockClusters() });
      } else {
        this.setData({ clusters: clu });
      }
    } catch (err) {
      this.setData({ heatmap: this.mockHeatmap(), clusters: this.mockClusters() });
    } finally {
      this.setData({ loading: false });
    }
  },

  mockHeatmap() {
    return [
      { region: '东兴口岸', level: 3, count: 42 },
      { region: '凭祥口岸', level: 3, count: 38 },
      { region: '龙邦口岸', level: 2, count: 21 },
      { region: '钦州港', level: 2, count: 19 },
      { region: '防城港', level: 1, count: 9 },
      { region: '友谊关', level: 2, count: 17 },
      { region: '水口口岸', level: 1, count: 7 },
      { region: '北海港', level: 0, count: 3 }
    ];
  },

  mockClusters() {
    return [
      { region: '东兴·边贸街区', count: 12, level: 'high', enterprises: ['东兴市众达贸易有限公司', '东兴市亚宏进出口贸易有限公司'] },
      { region: '凭祥·保税区', count: 8, level: 'medium', enterprises: ['凭祥市边贸通供应链有限公司', '崇左市东盟购电子商务有限公司'] },
      { region: '钦州港·临港', count: 5, level: 'low', enterprises: ['防城港市海拓进出口有限公司'] }
    ];
  },

  onShareAppMessage() {
    return { title: '东盟风控预警平台 · 协同监管后台', path: '/pages/regulate/regulate' };
  }
});
