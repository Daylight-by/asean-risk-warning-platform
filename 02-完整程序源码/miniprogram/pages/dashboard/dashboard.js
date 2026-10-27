const api = require('../../utils/api');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    loading: true,
    stats: {
      enterpriseTotal: 0,
      warningTotal: 0,
      policyTotal: 0,
      countryCount: 10
    },
    levelCounts: { high: 0, medium: 0, low: 0 },
    trend: [],
    recentWarnings: [],
    mapLat: 21.0,
    mapLng: 108.0,
    markers: [
      { id: 1, latitude: 22.817, longitude: 108.317, title: '南宁', width: 24, height: 24 },
      { id: 2, latitude: 21.545, longitude: 107.966, title: '东兴', width: 24, height: 24 },
      { id: 3, latitude: 21.028, longitude: 105.854, title: '河内', width: 24, height: 24 },
      { id: 4, latitude: 10.823, longitude: 106.629, title: '胡志明市', width: 24, height: 24 },
      { id: 5, latitude: 13.756, longitude: 100.501, title: '曼谷', width: 24, height: 24 },
      { id: 6, latitude: 1.352, longitude: 103.820, title: '新加坡', width: 24, height: 24 }
    ]
  },

  onLoad() {
    this.loadDashboard();
  },

  onShow() {
    if (this.data.loading) return;
    const recentWarnings = this.data.recentWarnings.map(item => ({
      ...item,
      levelText: this.levelText(item.level),
      createTime: item.createTimeTs ? this.formatTime(item.createTimeTs) : item.createTime
    }));
    this.setData({ recentWarnings }, () => {
      this.drawDonut();
      this.drawTrend();
    });
  },

  onPullDownRefresh() {
    this.loadDashboard().then(() => wx.stopPullDownRefresh());
  },

  onShareAppMessage() {
    return {
      title: '东盟风控预警平台 · 数据看板',
      path: '/pages/dashboard/dashboard'
    };
  },

  async loadDashboard() {
    this.setData({ loading: true });
    try {
      const [overviewRes, warningRes, policyRes] = await Promise.all([
        api.getDashboardOverview(),
        api.getWarningList(1, 50),
        api.getPolicyCount()
      ]);

      const overview = overviewRes.data || overviewRes || {};
      const list = warningRes.data?.list || warningRes.data || [];
      const policyData = policyRes.data || policyRes || {};

      const mapped = list.map(it => {
        const lvl = it.level ||
          (it.warningLevel === '高风险' || it.warningLevel === 'high' ? 'high'
            : it.warningLevel === '中风险' || it.warningLevel === 'medium' ? 'medium' : 'low');
        // 【面试】createTime兼容毫秒时间戳(number)和ISO字符串两种格式
        let ts = it.createTimeTs || 0;
        if (!ts && it.createTime) {
          ts = typeof it.createTime === 'number' ? it.createTime : new Date(it.createTime).getTime();
        }
        return { level: lvl, createTimeTs: ts };
      });

      const { levelCounts, trend } = this.processWarnings(mapped);

      const recent = list.slice(0, 5).map(it => {
        const lvl = it.level ||
          (it.warningLevel === '高风险' || it.warningLevel === 'high' ? 'high'
            : it.warningLevel === '中风险' || it.warningLevel === 'medium' ? 'medium' : 'low');
        let ts = it.createTimeTs || 0;
        if (!ts && it.createTime) {
          ts = typeof it.createTime === 'number' ? it.createTime : new Date(it.createTime).getTime();
        }
        if (!ts) ts = Date.now();
        return {
          enterpriseName: it.enterpriseName || '未知企业',
          level: lvl,
          levelText: this.levelText(lvl),
          content: it.content || it.warningContent || '--',
          createTimeTs: ts,
          createTime: this.formatTime(ts)
        };
      });

      this.setData({
        stats: {
          enterpriseTotal: overview.enterpriseTotal || overview.total || 0,
          warningTotal: overview.warningTotal || list.length,
          policyTotal: overview.policyTotal || policyData.total || 0,
          countryCount: overview.countryCount || 10
        },
        levelCounts,
        trend,
        recentWarnings: recent,
        loading: false
      }, () => {
        this.drawDonut();
        this.drawTrend();
      });
    } catch (err) {
      this.useMock();
    }
  },

  // 接口不可用时使用本地模拟数据（不影响现有功能）
  useMock() {
    const list = this.getMockWarnings();
    const { levelCounts, trend } = this.processWarnings(list);
    this.setData({
      stats: {
        enterpriseTotal: 1286,
        warningTotal: list.length,
        policyTotal: 8,
        countryCount: 10
      },
      levelCounts,
      trend,
      recentWarnings: list.slice(0, 5),
      loading: false
    }, () => {
      this.drawDonut();
      this.drawTrend();
    });
  },

  processWarnings(list) {
    const levelCounts = { high: 0, medium: 0, low: 0 };
    list.forEach(it => {
      if (it.level === 'high') levelCounts.high++;
      else if (it.level === 'medium') levelCounts.medium++;
      else levelCounts.low++;
    });

    const days = 14;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const trend = [];
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(d.getDate() - i);
      const label = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      trend.push({ label, count: 0 });
    }
    list.forEach(it => {
      if (!it.createTimeTs) return;
      const d = new Date(it.createTimeTs);
      const label = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      const t = trend.find(x => x.label === label);
      if (t) t.count++;
    });

    return { levelCounts, trend };
  },

  getMockWarnings() {
    const now = Date.now();
    const levels = ['high', 'medium', 'low', 'medium', 'high', 'low', 'medium', 'low', 'high', 'medium', 'low', 'medium', 'high', 'low', 'medium', 'low', 'high', 'medium', 'low', 'medium'];
    const names = ['东兴市众达贸易有限公司', '广西东兴淘一二三进出口贸易有限公司', '东兴市庆龙网络科技有限公司', '东兴市亚宏进出口贸易有限公司', '东兴市中越泰国际食品有限公司', '凭祥市边贸通供应链有限公司', '防城港市海拓进出口有限公司', '崇左市东盟购电子商务有限公司'];
    const contents = ['报关货值异常增长，涉嫌虚假出口退税。', '申报单价偏离市场均价，存在低值高报嫌疑。', '与境外交易频率异常，疑似规避监管。', '法定代表人变更，实控人未变。', '缺少境外食品安全许可，资质不全。', '口岸查验异常率偏高。', '行政处罚记录未整改。', '经营异常名录未移除。'];
    const list = [];
    for (let i = 0; i < 20; i++) {
      const dayIdx = Math.floor(i / 1.4);
      const offset = dayIdx * 86400000 + (i % 3) * 3600000;
      const ts = now - offset;
      list.push({
        enterpriseName: names[i % names.length],
        level: levels[i % levels.length],
        levelText: this.levelText(levels[i % levels.length]),
        content: contents[i % contents.length],
        createTimeTs: ts,
        createTime: this.formatTime(ts)
      });
    }
    return list;
  },

  levelText(level) {
    const common = this.data.t.common;
    return level === 'high' ? common.high : level === 'medium' ? common.medium : common.low;
  },

  formatTime(timestamp) {
    const now = new Date();
    const date = new Date(timestamp);
    const diff = now - date;
    const common = this.data.t.common;
    if (diff < 3600000) return Math.floor(diff / 60000) + common.minutesAgo;
    if (diff < 86400000) return Math.floor(diff / 3600000) + common.hoursAgo;
    if (diff < 2592000000) return Math.floor(diff / 86400000) + common.daysAgo;
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  // 风险等级分布环形图
  drawDonut() {
    const query = wx.createSelectorQuery();
    query.select('#donutCanvas').fields({ node: true, size: true }).exec((res) => {
      if (!res[0] || !res[0].node) return;
      const canvas = res[0].node;
      const ctx = canvas.getContext('2d');
      const dpr = this._dpr || (this._dpr = (wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : wx.getSystemInfoSync().pixelRatio));
      canvas.width = 300 * dpr;
      canvas.height = 300 * dpr;
      ctx.scale(dpr, dpr);

      const cx = 150, cy = 150, r = 100, lw = 28;
      ctx.clearRect(0, 0, 300, 300);

      const { high, medium, low } = this.data.levelCounts;
      const total = high + medium + low;

      if (total === 0) {
        ctx.beginPath();
        ctx.arc(cx, cy, r, 0, 2 * Math.PI);
        ctx.lineWidth = lw;
        ctx.strokeStyle = '#E8EEF3';
        ctx.stroke();
      } else {
        let start = -Math.PI / 2;
        const segs = [
          { v: high, c: '#EF4444' },
          { v: medium, c: '#F59E0B' },
          { v: low, c: '#10B981' }
        ];
        segs.forEach(s => {
          if (s.v <= 0) return;
          const angle = (s.v / total) * 2 * Math.PI;
          ctx.beginPath();
          ctx.arc(cx, cy, r, start, start + angle);
          ctx.lineWidth = lw;
          ctx.strokeStyle = s.c;
          ctx.lineCap = 'butt';
          ctx.stroke();
          start += angle;
        });
      }

      ctx.fillStyle = '#333';
      ctx.font = 'bold 40px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(String(total), cx, cy - 8);
      ctx.fillStyle = '#999';
      ctx.font = '20px sans-serif';
      ctx.fillText(this.data.t.common.riskEvents, cx, cy + 24);
    });
  },

  // 近14天预警趋势柱状图
  drawTrend() {
    const query = wx.createSelectorQuery();
    query.select('#trendCanvas').fields({ node: true, size: true }).exec((res) => {
      if (!res[0] || !res[0].node) return;
      const canvas = res[0].node;
      const ctx = canvas.getContext('2d');
      const dpr = this._dpr || (this._dpr = (wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : wx.getSystemInfoSync().pixelRatio));
      const W = 660, H = 300;
      canvas.width = W * dpr;
      canvas.height = H * dpr;
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, W, H);

      const trend = this.data.trend;
      const padL = 12, padR = 12, padT = 20, padB = 32;
      const cw = W - padL - padR;
      const ch = H - padT - padB;
      const max = Math.max(1, ...trend.map(t => t.count));
      const n = trend.length;
      const gap = cw / n;
      const bw = gap * 0.55;

      ctx.strokeStyle = '#EEE';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(padL, padT + ch);
      ctx.lineTo(padL + cw, padT + ch);
      ctx.stroke();

      trend.forEach((t, i) => {
        const x = padL + gap * i + (gap - bw) / 2;
        const bh = (t.count / max) * ch;
        const y = padT + ch - bh;
        const grad = ctx.createLinearGradient(0, y, 0, padT + ch);
        grad.addColorStop(0, '#1E4976');
        grad.addColorStop(1, '#4D8AC4');
        ctx.fillStyle = grad;
        ctx.fillRect(x, y, bw, bh);
        if (i % 2 === 0 || i === n - 1) {
          ctx.fillStyle = '#999';
          ctx.font = '16px sans-serif';
          ctx.textAlign = 'center';
          ctx.fillText(t.label, x + bw / 2, padT + ch + 20);
        }
      });
    });
  }
});
