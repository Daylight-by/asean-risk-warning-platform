const api = require('../../utils/api');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    enterpriseId: '',
    enterpriseName: '',
    loading: false,
    hasData: false,
    showSearch: false,
    searchKeyword: '',
    suggestList: [],
    resultList: [],
    reportInfo: {}
  },

  onLoad(options) {
    const { enterpriseId, enterpriseName } = options;
    if (enterpriseId) {
      this.setData({
        enterpriseId: enterpriseId,
        enterpriseName: decodeURIComponent(enterpriseName || '')
      });
      this.loadReport();
    } else {
      this.setData({ showSearch: true });
    }
  },

  onUnload() {
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
  },

  onShow() {
    if (!this.data.hasData || !this.data.reportInfo) return;
    const report = this.data.reportInfo;
    const labels = this.data.t.pages.report.dimensionNames || [];
    this.setData({
      'reportInfo.riskLevelText': this._riskLevelText(report.riskLevelKey),
      'reportInfo.dimensions': (report.dimensions || []).map((item, index) => ({ ...item, name: labels[index] || item.name })),
      'reportInfo.riskItems': (report.riskItems || []).map(item => ({ ...item, levelText: this._riskLevelText(item.level) }))
    });
  },

  // ===== 搜索相关 =====
  onSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword });
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    if (keyword.length >= 2) {
      this.debounceTimer = setTimeout(() => this.fetchSuggest(keyword), 500);
    } else {
      this.setData({ suggestList: [], resultList: [] });
    }
  },

  async fetchSuggest(keyword) {
    try {
      const res = await api.searchEnterprise(keyword);
      this.setData({ suggestList: res.data || [] });
    } catch (err) { console.error('搜索失败:', err); }
  },

  onSearch() {
    const keyword = this.data.searchKeyword.trim();
    if (!keyword || keyword.length < 2) {
      wx.showToast({ title: this.data.t.search.minChars, icon: 'none' });
      return;
    }
    this.fetchSuggest(keyword);
  },

  onSelectEnterprise(e) {
    const item = e.currentTarget.dataset.item;
    const enterpriseId = item.creditCode || item.id;
    const enterpriseName = item.enterpriseName || item.name || '';
    this.setData({
      enterpriseId, enterpriseName,
      showSearch: false, searchKeyword: '', suggestList: [], resultList: []
    });
    this.loadReport();
  },

  onClearSearch() {
    this.setData({ searchKeyword: '', suggestList: [], resultList: [] });
  },

  // ===== 报告加载 =====
  async loadReport() {
    this.setData({ loading: true });
    try {
      const [searchRes, scoreRes] = await Promise.all([
        api.searchEnterprise(this.data.enterpriseId),
        api.getRiskScore(this.data.enterpriseId)
      ]);
      const entList = searchRes.data || searchRes || [];
      const ent = Array.isArray(entList) ? entList[0] : entList;
      const scoreData = scoreRes.data || scoreRes || {};

      if (scoreData.code === 500 || scoreData.code === 404) {
        this.setData({
          reportInfo: this.buildReport(ent, { riskScore: 0, riskLevel: '未知', riskTags: [], basicScore: 0, complianceScore: 0, customsScore: 0, tradeScore: 0, adjustScore: 0 }),
          hasData: true, loading: false
        });
        return;
      }
      this.setData({ reportInfo: this.buildReport(ent, scoreData), hasData: true });
    } catch (err) {
      wx.showToast({ title: '加载报告失败，请检查网络', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  buildReport(ent, scoreData) {
    const score = scoreData.riskScore || 0;
    const backendLevel = scoreData.riskLevel || '';
    const riskLevelKey = this._riskLevelKey(backendLevel);
    const riskLevelText = this._riskLevelText(riskLevelKey);
    const dimensionNames = this.data.t.pages.report.dimensionNames || [];

    return {
      reportNo: 'RPT' + Date.now(),
      generateTime: this.formatDate(new Date()),
      enterpriseName: ent ? (ent.enterpriseName || ent.enterprise_name || this.data.enterpriseName) : this.data.enterpriseName || '未知企业',
      regNo: ent ? (ent.creditCode || ent.credit_code || this.data.enterpriseId) : this.data.enterpriseId || '--',
      legalPerson: ent ? (ent.legalPerson || ent.legal_person || '--') : '--',
      regCapital: ent ? ((ent.registeredCapital || ent.registered_capital || 0) + '万元') : '--',
      establishDate: ent ? (ent.establishDate || ent.establish_date || '--') : '--',
      country: '中国',
      status: this.data.language === 'zh' ? '正常' : this.data.language === 'vi' ? 'Bình thường' : 'Normal',
      statusKey: 'normal',
      businessScope: ent ? (ent.businessScope || ent.business_scope || '--') : '--',
      riskScore: score,
      riskLevelKey,
      riskLevelText,
      dimensions: [
        { name: dimensionNames[0], value: scoreData.basicScore || 0, color: '#409EFF' },
        { name: dimensionNames[1], value: scoreData.complianceScore || 0, color: '#67C23A' },
        { name: dimensionNames[2], value: scoreData.customsScore || 0, color: '#E6A23C' },
        { name: dimensionNames[3], value: scoreData.tradeScore || 0, color: '#9B59B6' },
        { name: dimensionNames[4], value: scoreData.adjustScore || 0, color: '#F56C6C' }
      ],
      riskItems: (scoreData.riskTags || []).map(tag => {
        let level = 'medium';
        if (tag.includes('高') || tag.includes('严重')) level = 'high';
        const levelText = this._riskLevelText(level);
        return { title: tag.split('（')[0], description: tag, date: this.formatDate(new Date()), level, levelText };
      })
    };
  },

  _riskLevelKey(level) {
    const value = String(level || '').toLowerCase();
    if (value === 'high' || level === '高风险') return 'high';
    if (value === 'medium' || level === '中风险') return 'medium';
    return 'low';
  },

  _riskLevelText(level) {
    return this.data.t.common[this._riskLevelKey(level)];
  },

  formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${d} ${h}:${min}`;
  },

  onGenerate() {
    if (this.data.enterpriseId) this.loadReport();
    else this.setData({ showSearch: true });
  },

  // 保存报告为图片
  async onSavePoster() {
    if (!this.data.hasData) {
      wx.showToast({ title: '请先生成报告', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '生成中...' });
    try {
      const canvas = await this.getCanvasNode('#posterCanvas');
      const ctx = canvas.getContext('2d');
      const dpr = this._dpr || (this._dpr = (wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : wx.getSystemInfoSync().pixelRatio));
      const W = 600, H = 920;
      canvas.width = W * dpr;
      canvas.height = H * dpr;
      ctx.scale(dpr, dpr);

      const info = this.data.reportInfo;
      const levelColor = info.riskLevelText === '高风险' ? '#F56C6C'
        : info.riskLevelText === '中风险' ? '#E6A23C' : '#67C23A';

      // 背景
      ctx.fillStyle = '#F5F7FA';
      ctx.fillRect(0, 0, W, H);

      // 头部
      ctx.fillStyle = '#2B6FF7';
      ctx.fillRect(0, 0, W, 160);
      ctx.textAlign = 'left';
      ctx.textBaseline = 'middle';
      ctx.fillStyle = '#FFFFFF';
      ctx.font = 'bold 34px sans-serif';
      ctx.fillText('企业信用报告', 40, 60);
      ctx.font = '22px sans-serif';
      ctx.fillStyle = 'rgba(255,255,255,0.9)';
      this.clipText(ctx, info.enterpriseName || '未知企业', 40, 105, W - 80);
      ctx.font = '18px sans-serif';
      ctx.fillStyle = 'rgba(255,255,255,0.6)';
      ctx.fillText('报告编号：' + (info.reportNo || '--'), 40, 140);

      // 评分卡片
      const cardY = 190;
      this.roundRect(ctx, 30, cardY, W - 60, 150, 16);
      ctx.fillStyle = '#FFFFFF';
      ctx.fill();
      ctx.fillStyle = '#666';
      ctx.font = '22px sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText('风控评分', 60, cardY + 36);
      ctx.textAlign = 'center';
      ctx.fillStyle = levelColor;
      ctx.font = 'bold 72px sans-serif';
      ctx.fillText(String(info.riskScore || 0), W / 2, cardY + 95);
      ctx.font = '24px sans-serif';
      ctx.fillText(info.riskLevelText || '低风险', W / 2, cardY + 130);

      // 基本信息
      let y = cardY + 200;
      const infos = [
        ['注册号', info.regNo || '--'],
        ['法定代表人', info.legalPerson || '--'],
        ['注册资本', info.regCapital || '--'],
        ['成立日期', info.establishDate || '--'],
        ['经营状态', info.status || '--']
      ];
      ctx.textAlign = 'left';
      infos.forEach(([k, v]) => {
        ctx.fillStyle = '#999';
        ctx.font = '20px sans-serif';
        ctx.fillText(k, 60, y);
        ctx.fillStyle = '#333';
        ctx.font = '22px sans-serif';
        this.clipText(ctx, v, 200, y, W - 240);
        y += 44;
      });

      // 风险维度
      y += 8;
      ctx.fillStyle = '#2B6FF7';
      ctx.font = 'bold 24px sans-serif';
      ctx.fillText('风险维度分析', 60, y);
      y += 30;
      (info.dimensions || []).forEach(d => {
        ctx.fillStyle = '#666';
        ctx.font = '20px sans-serif';
        ctx.fillText(d.name, 60, y + 10);
        ctx.fillStyle = '#EEE';
        ctx.fillRect(200, y, W - 280, 16);
        ctx.fillStyle = d.color || '#2B6FF7';
        ctx.fillRect(200, y, (W - 280) * (d.value / 100), 16);
        ctx.fillStyle = '#333';
        ctx.font = '20px sans-serif';
        ctx.textAlign = 'right';
        ctx.fillText(d.value + '分', W - 60, y + 10);
        ctx.textAlign = 'left';
        y += 40;
      });

      // 风险事项
      if (info.riskItems && info.riskItems.length) {
        y += 8;
        ctx.fillStyle = '#2B6FF7';
        ctx.font = 'bold 24px sans-serif';
        ctx.fillText('风险事项', 60, y);
        y += 34;
        info.riskItems.slice(0, 3).forEach(it => {
          ctx.fillStyle = it.level === 'high' ? '#F56C6C' : it.level === 'medium' ? '#E6A23C' : '#67C23A';
          this.clipText(ctx, '• ' + it.title, 60, y, W - 120);
          y += 34;
        });
      }

      // 底部
      ctx.fillStyle = '#BBB';
      ctx.font = '18px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('东盟风控预警平台 · 脱敏样例演示，结论待权威数据核验', W / 2, H - 30);

      const temp = await this.canvasToTemp(canvas);
      await this.saveToAlbum(temp);
      wx.hideLoading();
      wx.showToast({ title: '已保存到相册', icon: 'success' });
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '生成失败', icon: 'none' });
    }
  },

  clipText(ctx, text, x, y, maxWidth) {
    let t = String(text || '');
    if (ctx.measureText(t).width > maxWidth) {
      while (t.length > 0 && ctx.measureText(t + '…').width > maxWidth) t = t.slice(0, -1);
      t += '…';
    }
    ctx.fillText(t, x, y);
  },

  roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  },

  getCanvasNode(selector) {
    return new Promise((resolve, reject) => {
      wx.createSelectorQuery().select(selector).fields({ node: true, size: true }).exec(res => {
        if (res[0] && res[0].node) resolve(res[0].node);
        else reject(new Error('canvas not found'));
      });
    });
  },

  canvasToTemp(canvas) {
    return new Promise((resolve, reject) => {
      wx.canvasToTempFilePath({ canvas, success: r => resolve(r.tempFilePath), fail: reject });
    });
  },

  async saveToAlbum(filePath) {
    try {
      await new Promise((res, rej) => wx.saveImageToPhotosAlbum({ filePath, success: res, fail: rej }));
    } catch (e) {
      if (e.errMsg && e.errMsg.indexOf('auth deny') >= 0) {
        wx.showModal({
          title: '提示',
          content: '需要授权保存图片到相册',
          confirmText: '去设置',
          success: r => { if (r.confirm) wx.openSetting(); }
        });
      }
      throw e;
    }
  },

  onShareAppMessage() {
    return {
      title: '东盟风控预警平台 · 企业信用报告',
      path: '/pages/report/report'
    };
  }
});
