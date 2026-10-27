const api = require('../../utils/api');
const i18n = require('../../utils/i18n');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    enterpriseName: '',
    category: '',
    countries: ['越南', '泰国', '马来西亚', '印度尼西亚', '新加坡'],
    countryLabels: [],
    countryIndex: 0,
    loading: false,
    report: null
  },

  onLoad() { this.refreshCountryLabels(); },
  onShow() { this.refreshCountryLabels(); },

  refreshCountryLabels() {
    this.setData({ countryLabels: this.data.countries.map(country => i18n.displayCountry(country)) });
  },

  onInputName(e) { this.setData({ enterpriseName: e.detail.value }); },
  onInputCategory(e) { this.setData({ category: e.detail.value }); },
  onPickCountry(e) { this.setData({ countryIndex: Number(e.detail.value) }); },

  async onSubmit() {
    const name = this.data.enterpriseName.trim();
    if (!name) {
      wx.showToast({ title: this.data.t.pages.compliance.namePlaceholder, icon: 'none' });
      return;
    }
    const country = this.data.countries[this.data.countryIndex];
    const category = this.data.category.trim() || '农产品';
    this.setData({ loading: true });
    try {
      const res = await api.complianceCheck({ enterpriseName: name, country, category });
      if (res.code === 500 || res.code === 404) {
        this.loadMock(name, country, category);
      } else {
        const report = res.data || res;
        this.setData({ report: { ...report, countryDisplay: i18n.displayCountry(report.country || country) } });
      }
    } catch (err) {
      this.loadMock(name, country, category);
    } finally {
      this.setData({ loading: false });
    }
  },

  // 接口不可用时本地演示（中小微边贸合规体检报告）
  loadMock(name, country, category) {
    const cfg = {
      '越南': { qual: ['工贸证（进出口经营权）', 'EVFTA原产地证申领资质'], tariff: ['部分农产品享零关税，需Form E原产地证', '纺织品受配额管理'], redline: ['禁止低报货值', '原产地证信息须与报关一致'], score: 78 },
      '泰国': { qual: ['泰国FDA食品进口许可（食品类）', 'BOI投资优惠备案'], tariff: ['跨境电商B2C超180万泰铢需增值税登记', '农产品关税阶梯'], redline: ['禁止虚假申报货值', '进口食品须泰文标签'], score: 82 },
      '马来西亚': { qual: ['进口许可证（受控品类）', '棕榈油等大宗商品贸易资质'], tariff: ['棕榈油出口受MPOB监管', 'RCEP关税优惠适用'], redline: ['禁止无证出口大宗商品', '外资持股比例限制'], score: 75 },
      '印度尼西亚': { qual: ['本地外资持股限制合规', 'SNI强制认证（部分品类）'], tariff: ['镍矿等资源出口受限', '进口许可证（PI）'], redline: ['禁止违规出口资源类', '本地成分要求'], score: 70 },
      '新加坡': { qual: ['自由贸易港备案', 'TradeNet系统注册'], tariff: ['绝大多数商品零关税', '烟酒高消费税'], redline: ['禁止虚报品名', '受控化学品需许可'], score: 88 }
    };
    const c = cfg[country] || cfg['越南'];
    this.setData({
      report: {
        enterpriseName: name,
        country,
        countryDisplay: i18n.displayCountry(country),
        category,
        score: c.score,
        level: c.score >= 85 ? '合规良好' : c.score >= 75 ? '基本合规' : '需重点关注',
        qualification: c.qual,
        tariffRisk: c.tariff,
        customsRedline: c.redline,
        suggestion: `出口${country}的${category}贸易，建议优先办理${c.qual[0]}，并严格核对原产地证与报关一致性，规避低报货值风险。`
      }
    });
  },

  onShareAppMessage() {
    return { title: '东盟风控预警平台 · 边贸合规体检', path: '/pages/compliance/compliance' };
  }
});
