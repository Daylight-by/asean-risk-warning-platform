const api = require('../../utils/api');
const i18n = require('../../utils/i18n');
const translation = require('../../utils/translation-demo');

Page({
  behaviors: [require('../../utils/i18n-page')],
  data: {
    warningList: [],
    page: 1,
    pageSize: 10,
    loading: false,
    hasMore: true,
    highCount: 0,
    mediumCount: 0,
    lowCount: 0,
    predictive: null,
    showOriginal: false
  },

  onLoad() {
    this.loadWarningList();
  },

  onShow() {
    // 标记预警为已查看
    wx.setStorageSync('lastViewWarningTime', Date.now());
    this.refreshDisplayLabels();
    // 节流：距上次刷新 < 60s 不重复加载，避免切 tab 频繁闪屏；手动下拉仍可刷新
    const now = Date.now();
    if (this._lastRefresh && now - this._lastRefresh < 60000) return;
    // 每次显示页面刷新
    this.setData({ page: 1, warningList: [], hasMore: true });
    this.loadWarningList();
  },

  refreshDisplayLabels() {
    this.setData({ warningList: this.data.warningList.map(item => ({ ...translation.decorate(item, ['content']), levelText: i18n.riskLabel(item.level) })) });
  },

  onToggleOriginal() { this.setData({ showOriginal: !this.data.showOriginal }); },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 });
      this.loadWarningList();
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 1, warningList: [], hasMore: true });
    this.loadWarningList().then(() => wx.stopPullDownRefresh()).catch(() => wx.stopPullDownRefresh());
  },

  async loadWarningList() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    try {
      const res = await api.getWarningList(this.data.page, this.data.pageSize);
      const list = res.data?.list || res.data || [];
      
      const formattedList = list.map(item => {
        // 【面试】前后端字段映射：后端返回creditCode/warningLevel(snake_case)，前端统一转换为camelCase
        const creditCode = item.creditCode || item.credit_code || item.enterpriseId || '';
        const rawLevel = item.warningLevel || item.warning_level || item.level || 'medium';
        // 后端预警等级中文值 → 前端英文等级
        let level;
        if (rawLevel === 'high' || rawLevel === '严重' || rawLevel === '高风险') {
          level = 'high';
        } else if (rawLevel === 'medium' || rawLevel === '中风险') {
          level = 'medium';
        } else {
          level = 'low';
        }
        return translation.decorate({
          id: item.id,
          enterpriseId: creditCode,
          enterpriseName: item.enterpriseName || item.enterprise_name || '未知企业',
          level: level,
          levelText: i18n.riskLabel(level),
          content: item.content || item.warningContent || item.warning_content || item.title || '--',
          createTime: this.formatTime(item.createTime || item.create_time)
        }, ['content']);
      });

      const allList = this.data.page === 1 
        ? formattedList 
        : [...this.data.warningList, ...formattedList];

      let displayedList = allList;
      const locale = i18n.getLanguage();
      if (locale !== 'zh' && allList.length) {
        try {
          const translationRes = await api.getMultilingualContent('warning', locale, allList.map(item => item.id));
          const dictionary = (translationRes && translationRes.data) || {};
          displayedList = allList.map(item => {
            const fields = dictionary[String(item.id)] || {};
            return fields.content ? { ...item, displayContent: fields.content, translationStatus: fields.translationStatus, verificationStatus: fields.verificationStatus } : item;
          });
        } catch (_) { /* 本地词典继续兜底 */ }
      }

      // 计算统计
      const highCount = allList.filter(item => item.level === 'high').length;
      const mediumCount = allList.filter(item => item.level === 'medium').length;
      const lowCount = allList.filter(item => item.level === 'low').length;

      this.setData({
        warningList: displayedList.map(item => ({ ...translation.decorate(item, ['content']), displayContent: item.displayContent || translation.decorate(item, ['content']).displayContent })),
        hasMore: list.length >= this.data.pageSize,
        highCount,
        mediumCount,
        lowCount
      });

      // 跨境贸易欺诈前置预警（取首个高风险企业做时序预测演示）
      const high = allList.find(i => i.level === 'high');
      if (high) this.loadPredictive(high.enterpriseId, high.enterpriseName);
    } catch (err) {
      // API不可用，使用模拟数据
      if (this.data.page === 1) {
        const mockData = this.getMockData();
        const highCount = mockData.filter(item => item.level === 'high').length;
        const mediumCount = mockData.filter(item => item.level === 'medium').length;
        const lowCount = mockData.filter(item => item.level === 'low').length;
        this.setData({ 
          warningList: mockData.map(item => translation.decorate(item, ['content'])),
          highCount,
          mediumCount,
          lowCount
        });
      }
      this.setData({ hasMore: false });
    } finally {
      this.setData({ loading: false });
      this._lastRefresh = Date.now();
    }
  },

  getMockData() {
    const now = Date.now();
    return [
      {
        id: 'w001',
        enterpriseId: '91450681MA5QCDGM2X',
        enterpriseName: '东兴市众达贸易有限公司',
        level: 'high',
        levelText: i18n.riskLabel('high'),
        content: '该企业近3个月报关货值环比增长320%，与实际物流数据严重不匹配，涉嫌虚假出口退税。',
        createTime: this.formatTime(now - 3600000)
      },
      {
        id: 'w002',
        enterpriseId: '91450681MA5PPA1D5N',
        enterpriseName: '广西东兴淘一二三进出口贸易有限公司',
        level: 'medium',
        levelText: i18n.riskLabel('medium'),
        content: '该企业申报单价偏离同品类市场均价1.8个标准差，存在低值高报嫌疑。',
        createTime: this.formatTime(now - 7200000)
      },
      {
        id: 'w003',
        enterpriseId: '91450681MA5PGMT001',
        enterpriseName: '东兴市庆龙网络科技有限公司',
        level: 'high',
        levelText: i18n.riskLabel('high'),
        content: '该企业与越南供应商交易频率异常，单笔金额集中在申报阈值以下，疑似规避监管。',
        createTime: this.formatTime(now - 86400000)
      },
      {
        id: 'w004',
        enterpriseId: '91450681MA5N37BC0U',
        enterpriseName: '东兴市亚宏进出口贸易有限公司',
        level: 'low',
        levelText: i18n.riskLabel('low'),
        content: '该企业法定代表人变更，新旧法定代表人为同一实控人。',
        createTime: this.formatTime(now - 172800000)
      },
      {
        id: 'w005',
        enterpriseId: '91450681315843133T',
        enterpriseName: '东兴市中越泰国际食品有限公司',
        level: 'medium',
        levelText: i18n.riskLabel('medium'),
        content: '该企业缺少泰国FDA食品安全许可，进口食品加工资质不全。',
        createTime: this.formatTime(now - 259200000)
      }
    ];
  },

  formatTime(timestamp) {
    if (!timestamp || isNaN(new Date(timestamp).getTime())) return '未知时间';
    const now = new Date();
    const date = new Date(timestamp);
    const diff = now - date;

    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    if (diff < 2592000000) return Math.floor(diff / 86400000) + '天前';

    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  // 跨境贸易欺诈前置预警（时序预测，事前预警）
  async loadPredictive(enterpriseId, enterpriseName) {
    try {
      const res = await api.getPredictiveWarning(enterpriseId);
      if (res.code === 500 || res.code === 404) {
        this.setPredictiveMock(enterpriseName);
      } else {
        this.setData({ predictive: res.data || res });
      }
    } catch (err) {
      this.setPredictiveMock(enterpriseName);
    }
  },

  setPredictiveMock(name) {
    const language = i18n.getLanguage();
    const enterpriseName = translation.enterpriseAlias(name || '东兴市众达贸易有限公司');
    const copy = language === 'en'
      ? [{ type: 'False export-tax refund', prob: 86, level: 'high', desc: 'Abnormal declared-value fluctuation and logistics divergence in the past six months.' }, { type: 'Under/over invoicing', prob: 64, level: 'medium', desc: 'Declared unit price deviates materially from comparable market prices.' }, { type: 'Underground banking flow', prob: 41, level: 'low', desc: 'Settlement counterparties show frequent small-value split payments.' }]
      : language === 'vi'
        ? [{ type: 'Hoàn thuế xuất khẩu giả', prob: 86, level: 'high', desc: 'Biến động giá trị khai báo bất thường và chênh lệch với dữ liệu logistics.' }, { type: 'Khai thấp/cao giá trị', prob: 64, level: 'medium', desc: 'Đơn giá khai báo lệch đáng kể so với giá thị trường.' }, { type: 'Dòng tiền ngân hàng ngầm', prob: 41, level: 'low', desc: 'Đối tác thanh toán có dấu hiệu chia nhỏ giao dịch thường xuyên.' }]
        : [{ type: '虚假出口退税', prob: 86, level: 'high', desc: '近6月报关货值波动异常，退税申报与实际物流背离' }, { type: '低值高报 / 高值低报', prob: 64, level: 'medium', desc: '申报单价偏离市场均价超1.5个标准差' }, { type: '跨境地下钱庄资金流转', prob: 41, level: 'low', desc: '结算对手方涉及高频小额分拆收付' }];
    this.setData({
      predictive: {
        enterpriseName,
        risks: copy
      }
    });
  },

  onViewDetail(e) {
    const { enterpriseId, enterpriseName } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/score/score?enterpriseId=${enterpriseId}&enterpriseName=${encodeURIComponent(enterpriseName)}`
    });
  },

  onShareAppMessage() {
    return {
      title: '东盟风控预警平台 · 实时风险预警',
      path: '/pages/warning/warning'
    };
  }
});
