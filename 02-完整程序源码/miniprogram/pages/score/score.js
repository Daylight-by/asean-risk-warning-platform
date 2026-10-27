const api = require('../../utils/api');
const i18n = require('../../utils/i18n');

function scoreCopy(level) {
  const language = i18n.getLanguage();
  const copy = {
    zh: { high: '该企业存在较高经营风险，建议谨慎交易并密切关注预警信息。', medium: '该企业存在一定经营风险，建议适度关注并加强监控。', low: '该企业经营状况良好，信用记录优良，风险较低。' },
    en: { high: 'Higher operating risk identified. Trade cautiously and monitor alerts closely.', medium: 'Some operating risk identified. Strengthen monitoring before trading.', low: 'Business conditions are stable in this demo assessment, with relatively low risk.' },
    vi: { high: 'Phát hiện rủi ro hoạt động cao. Hãy giao dịch thận trọng và theo dõi sát cảnh báo.', medium: 'Có một số rủi ro hoạt động. Nên tăng cường giám sát trước khi giao dịch.', low: 'Điều kiện hoạt động ổn định theo đánh giá minh họa, rủi ro tương đối thấp.' }
  };
  return (copy[language] || copy.zh)[level];
}
function dimensionNames() {
  const names = {
    zh: ['基础资质', '经营合规', '口岸通关', '贸易活跃', '综合调整'],
    en: ['Basic profile', 'Compliance', 'Port clearance', 'Trade activity', 'Overall adjustment'],
    vi: ['Hồ sơ cơ bản', 'Tuân thủ', 'Thông quan', 'Hoạt động thương mại', 'Điều chỉnh tổng hợp']
  };
  return names[i18n.getLanguage()] || names.zh;
}

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    enterpriseId: '',
    enterpriseName: '',
    riskScore: 0,
    riskLevel: 'low',
    riskLevelText: '低风险',
    riskDescription: '',
    dimensions: [
      { name: '偿债能力', value: 0, color: '#F56C6C' },
      { name: '运营能力', value: 0, color: '#E6A23C' },
      { name: '盈利能力', value: 0, color: '#409EFF' },
      { name: '成长能力', value: 0, color: '#67C23A' },
      { name: '合规风险', value: 0, color: '#9B59B6' }
    ],
    riskTags: [],
    isSubscribed: false,
    countries: ['中国', '越南', '泰国', '马来西亚', '印度尼西亚'],
    countryIndex: 0,
    modelCountry: '中国'
  },

  onLoad(options) {
    const { enterpriseId, enterpriseName } = options;
    this.setData({
      enterpriseId: enterpriseId || '',
      enterpriseName: decodeURIComponent(enterpriseName || '未命名企业')
    });
    if (enterpriseId) {
      this.loadRiskScore();
      this.loadSubscribeStatus();
    } else {
      // 没有enterpriseId时使用mock数据，保证页面不空白
      this.loadMockData(this.data.countries[this.data.countryIndex]);
    }
  },

  onShow() {
    const names = dimensionNames();
    this.setData({
      riskLevelText: i18n.riskLabel(this.data.riskLevel),
      riskDescription: scoreCopy(this.data.riskLevel),
      dimensions: this.data.dimensions.map((item, index) => ({ ...item, name: names[index] || item.name }))
    }, () => this.drawRadar());
  },

  async loadRiskScore(country) {
    try {
      wx.showLoading({ title: '加载中...' });
      const c = country || this.data.countries[this.data.countryIndex];
      this.setData({ modelCountry: c });
      // 分国别差异化跨境风控评分模型（一国一策权重）
      const res = c === '中国'
        ? await api.getRiskScore(this.data.enterpriseId)
        : await api.getCountryRiskScore(this.data.enterpriseId, c);
      wx.hideLoading();

      // 后端返回业务错误码(如企业未找到)，降级到mock数据
      if (res.code === 500 || res.code === 404) {
        this.loadMockData(c);
        return;
      }

      const data = res.data || res;
      const score = data.riskScore || 0;

      // 直接使用后端返回的风险等级，不再前端重新计算（前端算法与后端不一致）
      const backendLevel = data.riskLevel || '';
      let riskLevel, riskLevelText, riskDescription;
      if (backendLevel === '高风险') {
        riskLevel = 'high';
        riskLevelText = i18n.riskLabel('high');
        riskDescription = scoreCopy('high');
      } else if (backendLevel === '中风险') {
        riskLevel = 'medium';
        riskLevelText = i18n.riskLabel('medium');
        riskDescription = scoreCopy('medium');
      } else {
        riskLevel = 'low';
        riskLevelText = i18n.riskLabel('low');
        riskDescription = scoreCopy('low');
      }

      const names = dimensionNames();
      const dimensions = [
        { name: names[0], value: data.basicScore || 0, color: '#409EFF' },
        { name: names[1], value: data.complianceScore || 0, color: '#67C23A' },
        { name: names[2], value: data.customsScore || 0, color: '#E6A23C' },
        { name: names[3], value: data.tradeScore || 0, color: '#9B59B6' },
        { name: names[4], value: data.adjustScore || 0, color: '#F56C6C' }
      ];

      const riskTags = (data.riskTags || []).map(tag => ({
        name: tag,
        level: 'medium'
      }));

      this.setData({
        riskScore: score,
        riskLevel,
        riskLevelText,
        riskDescription,
        dimensions,
        riskTags
      }, () => {
        // setData 回调里绘制，确保 DOM 已渲染完成
        this.drawGauge();
        this.drawRadar();
      });
    } catch (err) {
      wx.hideLoading();
      // 使用模拟数据展示
      this.loadMockData(c);
    }
  },

  // 分国别切换评分模型（一国一策）
  onPickCountry(e) {
    const idx = Number(e.detail.value);
    this.setData({ countryIndex: idx });
    if (this.data.enterpriseId) {
      this.loadRiskScore(this.data.countries[idx]);
    } else {
      this.loadMockData(this.data.countries[idx]);
    }
  },

  // 模拟数据（API不可用时），支持分国别差异化权重
  loadMockData(country) {
    const offsetMap = { '中国': 0, '越南': -8, '泰国': -4, '马来西亚': -6, '印度尼西亚': -12 };
    const off = offsetMap[country] || 0;
    // 后端不可用时仅展示固定的演示数据，避免同一企业每次查询分数随机变化。
    const demoScoreMap = { '中国': 82, '越南': 68, '泰国': 74, '马来西亚': 71, '印度尼西亚': 63 };
    let score = (demoScoreMap[country] || 70) + off;
    score = Math.max(8, Math.min(96, score));
    let riskLevel, riskLevelText, riskDescription;
    // 分数越高风险越低（与后端评分引擎一致）
    if (score <= 40) {
      riskLevel = 'high';
      riskLevelText = i18n.riskLabel('high');
      riskDescription = scoreCopy('high');
    } else if (score <= 70) {
      riskLevel = 'medium';
      riskLevelText = i18n.riskLabel('medium');
      riskDescription = scoreCopy('medium');
    } else {
      riskLevel = 'low';
      riskLevelText = i18n.riskLabel('low');
      riskDescription = scoreCopy('low');
    }

    this.setData({
      riskScore: score,
      riskLevel,
      riskLevelText,
      riskDescription,
      dimensions: dimensionNames().map((name, index) => ({ name, value: [84, 76, 70, 73, 78][index], color: ['#409EFF', '#67C23A', '#E6A23C', '#9B59B6', '#F56C6C'][index] })),
      riskTags: [
        { name: '存在1条经营异常记录', level: 'high' },
        { name: '口岸查验异常率偏高', level: 'medium' },
        { name: '成立年限较短', level: 'medium' }
      ]
    }, () => {
      this.drawGauge();
      this.drawRadar();
    });
  },

  async loadSubscribeStatus() {
    try {
      const res = await api.getSubscribeStatus(this.data.enterpriseId);
      this.setData({ isSubscribed: res.data?.subscribed || false });
    } catch (err) {
      console.error('获取订阅状态失败:', err);
    }
  },

  // 绘制仪表盘
  drawGauge() {
    const query = wx.createSelectorQuery();
    query.select('#gaugeCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0] || !res[0].node) return;
        
        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        const dpr = this._dpr || (this._dpr = (wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : wx.getSystemInfoSync().pixelRatio));

        const width = 320 * dpr;
        const height = 320 * dpr;
        canvas.width = width;
        canvas.height = height;
        ctx.scale(dpr, dpr);

        const cx = 160;
        const cy = 170;
        const radius = 120;
        const lineWidth = 20;

        ctx.clearRect(0, 0, 320, 320);

        // 背景圆弧
        ctx.beginPath();
        ctx.arc(cx, cy, radius, Math.PI, 2 * Math.PI);
        ctx.lineWidth = lineWidth;
        ctx.strokeStyle = '#F0F0F0';
        ctx.lineCap = 'round';
        ctx.stroke();

        // 底层彩色弧（三段：绿/黄/红）
        const greenEnd = Math.PI + (Math.PI * 0.33);   // 0-33分
        const yellowEnd = Math.PI + (Math.PI * 0.67);   // 33-67分

        // 绿色段
        ctx.beginPath();
        ctx.arc(cx, cy, radius, Math.PI, greenEnd);
        ctx.lineWidth = lineWidth;
        ctx.strokeStyle = '#67C23A';
        ctx.lineCap = 'butt';
        ctx.stroke();

        // 黄色段
        ctx.beginPath();
        ctx.arc(cx, cy, radius, greenEnd, yellowEnd);
        ctx.lineWidth = lineWidth;
        ctx.strokeStyle = '#E6A23C';
        ctx.lineCap = 'butt';
        ctx.stroke();

        // 红色段
        ctx.beginPath();
        ctx.arc(cx, cy, radius, yellowEnd, 2 * Math.PI);
        ctx.lineWidth = lineWidth;
        ctx.strokeStyle = '#F56C6C';
        ctx.lineCap = 'butt';
        ctx.stroke();

        // 指针
        const score = this.data.riskScore;
        const angle = Math.PI + (score / 100) * Math.PI;

        const pointerEndX = cx + Math.cos(angle) * (radius - 30);
        const pointerEndY = cy + Math.sin(angle) * (radius - 30);

        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(pointerEndX, pointerEndY);
        ctx.lineWidth = 4;
        ctx.strokeStyle = '#333';
        ctx.stroke();

        // 指针中心圆
        ctx.beginPath();
        ctx.arc(cx, cy, 10, 0, 2 * Math.PI);
        ctx.fillStyle = '#333';
        ctx.fill();
      });
  },

  // 绘制雷达图
  drawRadar() {
    const query = wx.createSelectorQuery();
    query.select('#radarCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0] || !res[0].node) return;

        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        const dpr = this._dpr || (this._dpr = (wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : wx.getSystemInfoSync().pixelRatio));

        const size = 500 * dpr;
        canvas.width = size;
        canvas.height = size;
        ctx.scale(dpr, dpr);

        const cx = 250;
        const cy = 240;
        const maxRadius = 180;
        const count = this.data.dimensions.length;
        const angleStep = (2 * Math.PI) / count;

        ctx.clearRect(0, 0, 500, 500);

        // 绘制网格（5层）
        for (let level = 1; level <= 5; level++) {
          const r = (maxRadius / 5) * level;
          ctx.beginPath();
          for (let i = 0; i < count; i++) {
            const angle = -Math.PI / 2 + i * angleStep;
            const x = cx + Math.cos(angle) * r;
            const y = cy + Math.sin(angle) * r;
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
          }
          ctx.closePath();
          ctx.strokeStyle = '#E8E8E8';
          ctx.lineWidth = 1;
          ctx.stroke();
        }

        // 绘制轴线
        for (let i = 0; i < count; i++) {
          const angle = -Math.PI / 2 + i * angleStep;
          ctx.beginPath();
          ctx.moveTo(cx, cy);
          ctx.lineTo(
            cx + Math.cos(angle) * maxRadius,
            cy + Math.sin(angle) * maxRadius
          );
          ctx.strokeStyle = '#E8E8E8';
          ctx.lineWidth = 1;
          ctx.stroke();
        }

        // 绘制标签
        ctx.font = '24px sans-serif';
        ctx.fillStyle = '#666';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';

        for (let i = 0; i < count; i++) {
          const angle = -Math.PI / 2 + i * angleStep;
          const labelR = maxRadius + 36;
          const x = cx + Math.cos(angle) * labelR;
          const y = cy + Math.sin(angle) * labelR;
          ctx.fillText(this.data.dimensions[i].name, x, y);
        }

        // 绘制数据区域
        ctx.beginPath();
        for (let i = 0; i < count; i++) {
          const angle = -Math.PI / 2 + i * angleStep;
          const val = this.data.dimensions[i].value;
          const r = (val / 100) * maxRadius;
          const x = cx + Math.cos(angle) * r;
          const y = cy + Math.sin(angle) * r;
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        }
        ctx.closePath();
        ctx.fillStyle = 'rgba(43, 111, 247, 0.12)';
        ctx.fill();
        ctx.strokeStyle = '#2B6FF7';
        ctx.lineWidth = 3;
        ctx.stroke();

        // 绘制数据点
        for (let i = 0; i < count; i++) {
          const angle = -Math.PI / 2 + i * angleStep;
          const val = this.data.dimensions[i].value;
          const r = (val / 100) * maxRadius;
          const x = cx + Math.cos(angle) * r;
          const y = cy + Math.sin(angle) * r;
          ctx.beginPath();
          ctx.arc(x, y, 6, 0, 2 * Math.PI);
          ctx.fillStyle = this.data.dimensions[i].color;
          ctx.fill();
        }
      });
  },

  // 生成报告
  onGenerateReport() {
    wx.showLoading({ title: '生成中...' });
    api.generateReport(this.data.enterpriseId).then(res => {
      wx.hideLoading();
      wx.navigateTo({
        url: `/pages/report/report?enterpriseId=${this.data.enterpriseId}&enterpriseName=${encodeURIComponent(this.data.enterpriseName)}`
      });
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '生成失败，展示示例报告', icon: 'none' });
      wx.navigateTo({
        url: `/pages/report/report?enterpriseId=${this.data.enterpriseId}&enterpriseName=${encodeURIComponent(this.data.enterpriseName)}`
      });
    });
  },

  // 订阅预警
  async onSubscribeWarning() {
    if (this.data.isSubscribed) {
      // 已订阅，取消确认
      wx.showModal({
        title: '取消订阅',
        content: '确认取消对该企业的预警订阅？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await api.unsubscribeWarning(this.data.enterpriseId);
              this.setData({ isSubscribed: false });
              this.removeSubscribe(this.data.enterpriseId);
              wx.showToast({ title: '已取消订阅', icon: 'success' });
            } catch (err) {
              wx.showToast({ title: '操作失败', icon: 'none' });
            }
          }
        }
      });
    } else {
      // 订阅
      try {
        await api.subscribeWarning(this.data.enterpriseId);
        this.setData({ isSubscribed: true });
        this.addSubscribe(this.data.enterpriseId, this.data.enterpriseName);
        wx.showToast({ title: '订阅成功', icon: 'success' });
      } catch (err) {
        wx.showToast({ title: '订阅失败', icon: 'none' });
      }
    }
  },

  addSubscribe(id, name) {
    let list = wx.getStorageSync('subscribeList') || [];
    if (!list.some(it => it.enterpriseId === id)) {
      list.push({ enterpriseId: id, enterpriseName: name });
      wx.setStorageSync('subscribeList', list);
    }
  },

  removeSubscribe(id) {
    let list = wx.getStorageSync('subscribeList') || [];
    list = list.filter(it => it.enterpriseId !== id);
    wx.setStorageSync('subscribeList', list);
  },

  onShareAppMessage() {
    return {
      title: '东盟风控预警平台 · 企业风控评分',
      path: '/pages/score/score'
    };
  }
});
